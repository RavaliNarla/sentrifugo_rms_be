package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.db.entity.*;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.enums.PreOnboardingStatus;
import com.bob.db.entity.JobPositionsEntity;
import com.bob.db.entity.JobRequisitionsEntity;
import com.bob.db.entity.ReservationCategoriesEntity;
import com.bob.db.repository.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class OnboardingService {

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private PreOnboardingRepository preOnboardingRepository;

    @Autowired
    private CandidateMeritListRepository candidateMeritListRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private PositionStateDistributionRepository positionStateDistributionRepository;

    @Autowired
    private PositionCategoryDistributionRepository positionCategoryDistributionRepository;

    @Autowired
    private PositionCategoryNationalDistributionRepository positionCategoryNationalDistributionRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    /**
     * Onboards a candidate:
     *  1. Validates application status = PRE_ONBOARDING_COMPLETED and candidateId match.
     *  2. Validates pre_onboarding onboarding_status = SUBMITTED and candidateId match.
     *  3. Loads merit list entry to get deduction keys.
     *  4. Deducts remaining vacancies across all relevant tables.
     *  5. Sets application status to ONBOARDED.
     */
    @Transactional
    public void onboardCandidate(UUID applicationId, UUID candidateId) {

        // 1. Validate candidate application
        CandidateApplicationsEntity application = candidateApplicationsRepository
                .findByApplicationIdIgnoringPaymentStatus(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate application not found: " + applicationId));

        if (!candidateId.equals(application.getCandidateId())) {
            throw new CommonException("Application does not belong to the provided candidate.");
        }
        if (application.getApplicationStatus() != CandidateApplicationStatus.PRE_ONBOARDING_COMPLETED) {
            throw new CommonException("Application must be in PRE_ONBOARDING_COMPLETED status to onboard. Current status: "
                    + application.getApplicationStatus());
        }

        // 2. Validate pre-onboarding
        PreOnboardingEntity preOnboarding = preOnboardingRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-onboarding record not found for application: " + applicationId));

        if (!candidateId.equals(preOnboarding.getCandidateId())) {
            throw new CommonException("Pre-onboarding record does not match the provided candidate.");
        }
        if (preOnboarding.getOnboardingStatus() != PreOnboardingStatus.SUBMITTED) {
            throw new CommonException("Pre-onboarding must be in SUBMITTED status. Current status: "
                    + preOnboarding.getOnboardingStatus());
        }

        // 3. Load merit list for deduction keys
        CandidateMeritListEntity meritEntry = candidateMeritListRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Merit list entry not found for application: " + applicationId));

        UUID positionId            = meritEntry.getPositionId();
        UUID selectedCategoryId    = meritEntry.getSelectedCategoryId();
        UUID selectedPwdCategoryId = meritEntry.getSelectedPwdCategoryId();
        UUID stateId               = meritEntry.getStateId();
        UUID cityId                = meritEntry.getCityId();
        Boolean isEwsSeat          = meritEntry.getIsEWSSeat();

        if (selectedCategoryId == null) {
            throw new CommonException("Merit list entry is missing selected_category_id for application: " + applicationId);
        }

        // If the candidate is placed in an EWS seat (regardless of their own reservation category),
        // we must deduct from the EWS vacancy bucket — not from their own category.
        // This covers both direct-EWS candidates (selected_category_id already = EWS UUID)
        // and GEN-in-EWS candidates (selected_category_id = GEN UUID but is_ews_seat = true).
        UUID effectiveCategoryId = selectedCategoryId;
        if (Boolean.TRUE.equals(isEwsSeat)) {
            UUID ewsCategoryId = reservationCategoriesRepository
                    .findByCategoryCodeIgnoreCase("EWS")
                    .map(ReservationCategoriesEntity::getId)
                    .orElseThrow(() -> new CommonException("EWS reservation category not found in master data."));
            effectiveCategoryId = ewsCategoryId;
        }

        // 4. Load position
        JobPositionsEntity position = positionsRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Job position not found: " + positionId));

        // 4a. Deduct position-level total vacancies
        deductPositionTotal(position);

        // 4b. Deduct distribution vacancies based on location type
        if (Boolean.TRUE.equals(position.getIsLocationWise())) {
            deductStateWise(positionId, stateId, cityId, effectiveCategoryId, selectedPwdCategoryId);
        } else {
            deductNational(positionId, effectiveCategoryId, selectedPwdCategoryId);
        }

        // 5. Update is_hiring_completed if this position's remaining vacancies hit 0
        if (position.getRemainingTotalVacancies() != null && position.getRemainingTotalVacancies() == 0) {
            position.setIsHiringCompleted(true);
            positionsRepository.save(position);
            log.info("All vacancies filled for position {}. Marking is_hiring_completed.", positionId);

            // If all sibling positions in the same req are also completed, mark the req too.
            List<JobPositionsEntity> siblings = positionsRepository.findAllByRequisitionId(position.getRequisitionId());
            boolean allDone = siblings.stream().allMatch(p -> Boolean.TRUE.equals(p.getIsHiringCompleted()));
            if (allDone) {
                jobRequisitionsRepository.findById(position.getRequisitionId()).ifPresent(req -> {
                    req.setIsHiringCompleted(true);
                    jobRequisitionsRepository.save(req);
                    log.info("All positions filled for requisition {}. Marking is_hiring_completed.", req.getId());
                });
            }
        }

        // 6. Mark as onboarded
        application.setApplicationStatus(CandidateApplicationStatus.ONBOARDED);
        candidateApplicationsRepository.save(application);

        log.info("Candidate {} onboarded for application {}. Position: {}", candidateId, applicationId, positionId);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Deduction helpers
    // ──────────────────────────────────────────────────────────────────────────────

    private void deductPositionTotal(JobPositionsEntity position) {
        if (position.getRemainingTotalVacancies() == null) {
            position.setRemainingTotalVacancies(position.getTotalVacancies());
        }
        if (position.getRemainingTotalVacancies() <= 0) {
            throw new CommonException("No remaining total vacancies for position: " + position.getId());
        }
        position.setRemainingTotalVacancies(position.getRemainingTotalVacancies() - 1);
        positionsRepository.save(position);
    }

    private void deductNational(UUID positionId, UUID selectedCategoryId, UUID selectedPwdCategoryId) {
        // Deduct main reservation-category row
        PositionCategoryNationalDistributionEntity mainRow =
                positionCategoryNationalDistributionRepository.findMainCategoryRow(positionId, selectedCategoryId)
                        .orElseThrow(() -> new CommonException(
                                "National category distribution not found for position " + positionId
                                + " and reservation category " + selectedCategoryId));
        deductNationalRow(mainRow);

        // Deduct disability sub-row (only if candidate is selected against a PWD category)
        if (selectedPwdCategoryId != null) {
            PositionCategoryNationalDistributionEntity pwdRow =
                    positionCategoryNationalDistributionRepository
                            .findDisabilityCategoryRow(positionId, selectedCategoryId, selectedPwdCategoryId)
                            .orElseThrow(() -> new CommonException(
                                    "National disability category distribution not found for position " + positionId
                                    + ", reservation category " + selectedCategoryId
                                    + ", disability category " + selectedPwdCategoryId));
            deductNationalRow(pwdRow);
        }
    }

    private void deductNationalRow(PositionCategoryNationalDistributionEntity row) {
        if (row.getRemainingVacancyCount() == null) {
            row.setRemainingVacancyCount(row.getVacancyCount());
        }
        if (row.getRemainingVacancyCount() <= 0) {
            throw new CommonException("No remaining vacancies in national distribution row: " + row.getId());
        }
        row.setRemainingVacancyCount(row.getRemainingVacancyCount() - 1);
        positionCategoryNationalDistributionRepository.save(row);
    }

    private void deductStateWise(UUID positionId, UUID stateId, UUID cityId,
                                  UUID selectedCategoryId, UUID selectedPwdCategoryId) {
        // Resolve state distribution entry: try state+city first, fall back to state-only
        PositionStateDistributionEntity stateDist = resolveStateDist(positionId, stateId, cityId);

        // Deduct state distribution total
        deductStateDistTotal(stateDist);

        // Deduct main category row under this state distribution
        PositionCategoryDistributionEntity mainRow =
                positionCategoryDistributionRepository.findMainCategoryRow(stateDist.getId(), selectedCategoryId)
                        .orElseThrow(() -> new CommonException(
                                "State category distribution not found for state dist " + stateDist.getId()
                                + " and reservation category " + selectedCategoryId));
        deductCategoryDistRow(mainRow);

        // Deduct disability sub-row
        if (selectedPwdCategoryId != null) {
            PositionCategoryDistributionEntity pwdRow =
                    positionCategoryDistributionRepository
                            .findDisabilityCategoryRow(stateDist.getId(), selectedCategoryId, selectedPwdCategoryId)
                            .orElseThrow(() -> new CommonException(
                                    "State disability category distribution not found for state dist " + stateDist.getId()
                                    + ", reservation category " + selectedCategoryId
                                    + ", disability category " + selectedPwdCategoryId));
            deductCategoryDistRow(pwdRow);
        }
    }

    private PositionStateDistributionEntity resolveStateDist(UUID positionId, UUID stateId, UUID cityId) {
        // Try exact state+city match first (only if cityId is present)
        if (cityId != null) {
            var exact = positionStateDistributionRepository.findByPositionAndStateAndCity(positionId, stateId, cityId);
            if (exact.isPresent()) {
                return exact.get();
            }
        }
        // Fall back to state-only entry
        return positionStateDistributionRepository.findByPositionAndStateWithoutCity(positionId, stateId)
                .orElseThrow(() -> new CommonException(
                        "No state distribution entry found for position " + positionId
                        + ", state " + stateId + ". The selected state does not exist in the job position data."));
    }

    private void deductStateDistTotal(PositionStateDistributionEntity stateDist) {
        if (stateDist.getRemainingTotalVacancies() == null) {
            stateDist.setRemainingTotalVacancies(stateDist.getTotalVacancies());
        }
        if (stateDist.getRemainingTotalVacancies() <= 0) {
            throw new CommonException("No remaining vacancies for state distribution: " + stateDist.getId());
        }
        stateDist.setRemainingTotalVacancies(stateDist.getRemainingTotalVacancies() - 1);
        positionStateDistributionRepository.save(stateDist);
    }

    private void deductCategoryDistRow(PositionCategoryDistributionEntity row) {
        if (row.getRemainingVacancyCount() == null) {
            row.setRemainingVacancyCount(row.getVacancyCount());
        }
        if (row.getRemainingVacancyCount() <= 0) {
            throw new CommonException("No remaining vacancies in state category distribution row: " + row.getId());
        }
        row.setRemainingVacancyCount(row.getRemainingVacancyCount() - 1);
        positionCategoryDistributionRepository.save(row);
    }
}
