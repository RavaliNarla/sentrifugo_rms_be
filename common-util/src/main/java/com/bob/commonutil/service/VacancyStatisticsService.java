package com.bob.commonutil.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.db.entity.*;
import com.bob.db.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Handles incrementing offer-related statistics (offers_sent, offers_accepted)
 * across position and vacancy distribution tables.
 *
 * Uses the same merit-list-driven path as OnboardingService:
 *   position → state/national distribution → category distribution
 *
 * Must be called from within an active transaction.
 */
@Service
@Slf4j
public class VacancyStatisticsService {

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

    @Transactional
    public void incrementOffersSent(UUID applicationId) {
        incrementStat(applicationId, StatType.OFFERS_SENT);
    }

    @Transactional
    public void incrementOffersAccepted(UUID applicationId) {
        incrementStat(applicationId, StatType.OFFERS_ACCEPTED);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private enum StatType { OFFERS_SENT, OFFERS_ACCEPTED }

    private void incrementStat(UUID applicationId, StatType type) {
        CandidateMeritListEntity merit = candidateMeritListRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Merit list entry not found for application: " + applicationId));

        UUID positionId            = merit.getPositionId();
        UUID selectedCategoryId    = merit.getSelectedCategoryId();
        UUID selectedPwdCategoryId = merit.getSelectedPwdCategoryId();
        UUID stateId               = merit.getStateId();
        UUID cityId                = merit.getCityId();
        Boolean isEwsSeat          = merit.getIsEWSSeat();

        if (selectedCategoryId == null) {
            log.warn("Merit list entry for application {} has no selected_category_id — skipping stat increment.", applicationId);
            return;
        }

        // Resolve effective reservation category (EWS override when is_ews_seat = true)
        UUID effectiveCategoryId = selectedCategoryId;
        if (Boolean.TRUE.equals(isEwsSeat)) {
            effectiveCategoryId = resolveEwsCategoryId();
        }

        JobPositionsEntity position = positionsRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + positionId));

        // Position-level
        incrementPositionStat(position, type);

        // Distribution-level
        if (Boolean.TRUE.equals(position.getIsLocationWise())) {
            incrementStateWise(positionId, stateId, cityId, effectiveCategoryId, selectedPwdCategoryId, type);
        } else {
            incrementNational(positionId, effectiveCategoryId, selectedPwdCategoryId, type);
        }
    }

    private void incrementPositionStat(JobPositionsEntity position, StatType type) {
        if (type == StatType.OFFERS_SENT) {
            position.setOffersSent((position.getOffersSent() == null ? 0 : position.getOffersSent()) + 1);
        } else {
            position.setOffersAccepted((position.getOffersAccepted() == null ? 0 : position.getOffersAccepted()) + 1);
        }
        positionsRepository.save(position);
    }

    private void incrementNational(UUID positionId, UUID categoryId, UUID pwdCategoryId, StatType type) {
        positionCategoryNationalDistributionRepository.findMainCategoryRow(positionId, categoryId)
                .ifPresent(row -> {
                    incrementNationalRow(row, type);
                    positionCategoryNationalDistributionRepository.save(row);
                });

        if (pwdCategoryId != null) {
            positionCategoryNationalDistributionRepository.findDisabilityCategoryRow(positionId, categoryId, pwdCategoryId)
                    .ifPresent(row -> {
                        incrementNationalRow(row, type);
                        positionCategoryNationalDistributionRepository.save(row);
                    });
        }
    }

    private void incrementNationalRow(PositionCategoryNationalDistributionEntity row, StatType type) {
        if (type == StatType.OFFERS_SENT) {
            row.setOffersSent((row.getOffersSent() == null ? 0 : row.getOffersSent()) + 1);
        } else {
            row.setOffersAccepted((row.getOffersAccepted() == null ? 0 : row.getOffersAccepted()) + 1);
        }
    }

    private void incrementStateWise(UUID positionId, UUID stateId, UUID cityId,
                                     UUID categoryId, UUID pwdCategoryId, StatType type) {
        PositionStateDistributionEntity stateDist = resolveStateDist(positionId, stateId, cityId);

        if (type == StatType.OFFERS_SENT) {
            stateDist.setOffersSent((stateDist.getOffersSent() == null ? 0 : stateDist.getOffersSent()) + 1);
        } else {
            stateDist.setOffersAccepted((stateDist.getOffersAccepted() == null ? 0 : stateDist.getOffersAccepted()) + 1);
        }
        positionStateDistributionRepository.save(stateDist);

        positionCategoryDistributionRepository.findMainCategoryRow(stateDist.getId(), categoryId)
                .ifPresent(row -> {
                    incrementCategoryRow(row, type);
                    positionCategoryDistributionRepository.save(row);
                });

        if (pwdCategoryId != null) {
            positionCategoryDistributionRepository.findDisabilityCategoryRow(stateDist.getId(), categoryId, pwdCategoryId)
                    .ifPresent(row -> {
                        incrementCategoryRow(row, type);
                        positionCategoryDistributionRepository.save(row);
                    });
        }
    }

    private void incrementCategoryRow(PositionCategoryDistributionEntity row, StatType type) {
        if (type == StatType.OFFERS_SENT) {
            row.setOffersSent((row.getOffersSent() == null ? 0 : row.getOffersSent()) + 1);
        } else {
            row.setOffersAccepted((row.getOffersAccepted() == null ? 0 : row.getOffersAccepted()) + 1);
        }
    }

    private PositionStateDistributionEntity resolveStateDist(UUID positionId, UUID stateId, UUID cityId) {
        if (cityId != null) {
            Optional<PositionStateDistributionEntity> exact =
                    positionStateDistributionRepository.findByPositionAndStateAndCity(positionId, stateId, cityId);
            if (exact.isPresent()) return exact.get();
        }
        return positionStateDistributionRepository.findByPositionAndStateWithoutCity(positionId, stateId)
                .orElseThrow(() -> new CommonException(
                        "No state distribution entry found for position " + positionId + ", state " + stateId));
    }

    private UUID resolveEwsCategoryId() {
        return reservationCategoriesRepository.findByCategoryCodeIgnoreCase("EWS")
                .map(ReservationCategoriesEntity::getId)
                .orElseThrow(() -> new CommonException("EWS reservation category not found in master data."));
    }
}
