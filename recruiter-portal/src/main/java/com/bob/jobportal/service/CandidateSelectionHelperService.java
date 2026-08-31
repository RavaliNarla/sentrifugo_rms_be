package com.bob.jobportal.service;

import com.bob.commonutil.model.VacancyMatrixDTO;
import com.bob.db.dto.JobPositionsDTO;
import com.bob.db.dto.PositionCategoryDistributionDTO;
import com.bob.db.dto.PositionCategoryNationalDistributionDTO;
import com.bob.db.dto.PositionStateDistributionDTO;
import com.bob.db.entity.CandidateConcessionsEntity;
import com.bob.db.entity.CandidateLocationPreferenceEntity;
import com.bob.db.entity.WrittenExamConfigurationEntity;
import com.bob.db.repository.CandidateConcessionsRepository;
import com.bob.db.repository.CandidateLocationPreferencesRepository;
import com.bob.db.repository.WrittenExamConfigurationRepository;
import com.bob.db.model.MeritCandidateDTO;
import com.bob.jobportal.model.VacancyMatrixModel;
import com.bob.jobportal.util.PwdCategoryCache;
import com.bob.jobportal.util.PwdTracker;
import com.bob.jobportal.util.ReservationCategoryCache;
import com.bob.jobportal.util.VacancyTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CandidateSelectionHelperService {

    @Autowired
    private CandidateConcessionsRepository candidateConcessionsRepository;

    @Autowired
    private WrittenExamConfigurationRepository writtenExamConfigurationRepository;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private PwdCategoryCache pwdCategoryCache;

    @Autowired
    private ReservationCategoryCache reservationCategoryCache;

    public List<MeritCandidateDTO> determineGeneralEligibility(List<MeritCandidateDTO> candidates) {
        for (MeritCandidateDTO candidate : candidates) {
            boolean eligible =!Boolean.TRUE.equals(candidate.getUsedAgeRelaxation())
                    && !Boolean.TRUE.equals(candidate.getUsedMarksRelaxation())
                    && !Boolean.TRUE.equals(candidate.getUsedInterviewRelaxation());

            candidate.setGeneralEligible(eligible);
        }
        return candidates;
    }

    public List<MeritCandidateDTO> loadLocationPreference(List<MeritCandidateDTO> candidates,Boolean isLocationWise,UUID positionId) {

        if (!isLocationWise) {
            return candidates;
        }

        List<CandidateLocationPreferenceEntity> preferenceEntities =
                candidateLocationPreferencesRepository.findAllByPositionId(positionId);

        Map<UUID, CandidateLocationPreferenceEntity> preferenceEntityMap =
                preferenceEntities.stream()
                        .collect(Collectors.toMap(
                                CandidateLocationPreferenceEntity::getCandidateId,
                                Function.identity()));

        candidates.forEach(candidate -> {
            CandidateLocationPreferenceEntity preference =
                    preferenceEntityMap.get(candidate.getCandidateId());

            if (preference != null) {
                candidate.setStateId(preference.getStatePreference1());
                candidate.setCityId(preference.getLocationPreference1());
            }
        });

        return candidates;
    }

    public List<MeritCandidateDTO> generateMasterMeritList(List<MeritCandidateDTO> candidates) {

        List<MeritCandidateDTO> qualifiedCandidates = candidates.stream()
                        .sorted(Comparator.comparing(MeritCandidateDTO::getCombinedScore,
                                Comparator.reverseOrder()).thenComparing(MeritCandidateDTO::getDob)).toList();

        int rank = 1;
        for(MeritCandidateDTO candidate : qualifiedCandidates) {
            candidate.setOverallRank(rank++);
        }
        return qualifiedCandidates;
    }

    public List<MeritCandidateDTO> fillInitialGeneralSeats(List<MeritCandidateDTO> meritList, VacancyTracker tracker,UUID generalCategoryId ) {

        for (MeritCandidateDTO candidate : meritList) {

            /*
             * ALREADY SELECTED
             */
            if (Boolean.TRUE.equals(candidate.getSelected())) {
                continue;
            }

            /*
             * NOT GENERAL ELIGIBLE
             */
            if (!Boolean.TRUE.equals(candidate.getGeneralEligible())) {
                continue;
            }

            /*
             * FILL GENERAL SEAT
             */
            VacancyTracker.LableAndSequence lableAndSequence = tracker.fillVacancy(candidate.getStateId(), candidate.getCityId(), generalCategoryId);

            /*
             * VACANCY FILLED
             */
            if (lableAndSequence != null) {

                candidate.setSelected(true);

                candidate.setSelectedCategoryId(
                        generalCategoryId
                );

                /*
                 * GEN-1
                 * GEN-2
                 */
                candidate.setSelectionLabel(lableAndSequence.getLabel());
                candidate.setCategoryRank(lableAndSequence.getSequence());

                /*
                 * CATEGORY MIGRATION
                 */
                if (!candidate.getCategoryId().equals(generalCategoryId)) {

                    candidate.setMigratedToGeneral(true);
                }
            }
        }

        return meritList;
    }

    public List<MeritCandidateDTO> fillConvertedGeneralSeats(List<MeritCandidateDTO> meritList, VacancyTracker tracker,UUID generalCategoryId ) {

        for (MeritCandidateDTO candidate : meritList) {

            /*
             * ALREADY SELECTED
             */
            if (Boolean.TRUE.equals(candidate.getSelected())) {
                continue;
            }

            /*
             * NOT GENERAL ELIGIBLE
             */
            if (!Boolean.TRUE.equals( candidate.getGeneralEligible())) {
                continue;
            }

            /*
             * FILL GENERAL SEAT
             */
            VacancyTracker.LableAndSequence lableAndSequence = tracker.fillVacancy(candidate.getStateId(), candidate.getCityId(), generalCategoryId);

            /*
             * VACANCY FILLED
             */
            if (lableAndSequence != null) {

                candidate.setSelected(true);

                candidate.setSelectedCategoryId(generalCategoryId);

                /*
                 * GEN-1
                 * GEN-2
                 */
                candidate.setSelectionLabel(lableAndSequence.getLabel());
                candidate.setCategoryRank(lableAndSequence.getSequence());
                candidate.setIsEWSSeat(lableAndSequence.getIsEWSSeat());

                /*
                 * CATEGORY MIGRATION
                 */
                if (!candidate.getCategoryId().equals(generalCategoryId)) {

                    candidate.setMigratedToGeneral(true);
                }
            }
        }

        return meritList;
    }

    public List<MeritCandidateDTO> fillEWSSeats(List<MeritCandidateDTO> meritList, VacancyTracker tracker, UUID ewsCategoryId, UUID generalCategoryId) {

        for (MeritCandidateDTO candidate : meritList) {

            /*
             * ALREADY SELECTED
             */
            if (Boolean.TRUE.equals(candidate.getSelected())) {
                continue;
            }

            /*
             * NOT EWS CATEGORY
             */
            if (candidate.getCategoryId() == null || !candidate.getCategoryId().equals(ewsCategoryId)) {
                continue;
            }

            /*
             * FILL GENERAL SEAT
             */
            VacancyTracker.LableAndSequence lableAndSequence = tracker.fillVacancy(candidate.getStateId(), candidate.getCityId(), ewsCategoryId);

            /*
             * VACANCY FILLED
             */
            if (lableAndSequence != null) {

                candidate.setSelected(true);

                candidate.setSelectedCategoryId(ewsCategoryId);

                /*
                 * GEN-1
                 * GEN-2
                 */
                candidate.setSelectionLabel(lableAndSequence.getLabel());
                candidate.setCategoryRank(lableAndSequence.getSequence());

                /*
                 * CATEGORY MIGRATION
                 */
                if (!candidate.getCategoryId().equals(ewsCategoryId)) {
                    candidate.setMigratedToGeneral(true);
                }
            }
        }

        tracker.transferAllRemainingVacancies(ewsCategoryId, generalCategoryId);


        return meritList;

    }

    public void occupyExistingSeats(VacancyMatrixModel vacancyMatrixModel, List<MeritCandidateDTO> meritListEntities) {
            VacancyTracker vacancyTracker = vacancyMatrixModel.getVacancyTracker();

            PwdTracker pwdTracker = vacancyMatrixModel.getPwdTracker();
            if (meritListEntities == null || meritListEntities.isEmpty()) { return; }

        for (MeritCandidateDTO merit : meritListEntities) {

            /*
             * OCCUPY VERTICAL SEAT
             */
            vacancyTracker.fillOccupyVacancy(merit.getStateId(), merit.getCityId(), merit.getSelectedCategoryId(),merit.getCategoryRank(), merit.getIsEWSSeat(),reservationCategoryCache);

            /*
             * OCCUPY HORIZONTAL PWD SEAT
             */
            if (Boolean.TRUE.equals(merit.getIsPwd()) && merit.getSelectedAgainstPwd() && merit.getSelectedPwdCategoryId() != null) {
                pwdTracker.fill( merit.getStateId(), merit.getCityId(), merit.getSelectedPwdCategoryId());
            }
        }
    }

//    public void addRejectedVacancies(VacancyMatrixModel vacancyMatrixModel, List<MeritCandidateDTO> rejectedCandidates) {
//
//        VacancyTracker vacancyTracker = vacancyMatrixModel.getVacancyTracker();
//
//        PwdTracker pwdTracker = vacancyMatrixModel.getPwdTracker();
//
//        if (rejectedCandidates == null || rejectedCandidates.isEmpty()) {
//            return;
//        }
//
//        for (MeritCandidateDTO merit : rejectedCandidates) {
//
//            /*
//             * ADD ONE MORE VERTICAL VACANCY
//             */
//            VacancyTracker.VacancySummary summary = vacancyTracker.getSummary(merit.getStateId(), merit.getCityId(), merit.getSelectedCategoryId());
//
//            if (summary != null) {
//                summary.setTotalVacancy(summary.getTotalVacancy() + 1);
//                summary.setFilledVacancy(summary.getFilledVacancy()+1);
//                summary.setUnfilledVacancy(summary.getUnfilledVacancy() + 1);
//                summary.getSequenceList().add(merit.getCategoryRank());
//                //summary.setNextSequence(summary.getNextSequence()+1);
//            }
//
//            if(merit.getIsEWSSeat() && merit.getSelectedCategoryId().equals(reservationCategoryCache.getGeneralCategoryId())){
//              VacancyTracker.VacancySummary ewsSummary =  vacancyTracker.getSummary(merit.getStateId(), merit.getCityId(), reservationCategoryCache.getEwsCategoryId());
//              if(ewsSummary != null) {
//                  summary.setEwsSeatCount(summary.getEwsSeatCount() + 1);
//                  summary.setTotalVacancy(summary.getTotalVacancy() + 1);
//                  ewsSummary.setTotalVacancy(ewsSummary.getTotalVacancy() - 1);
//                  summary.setUnfilledVacancy(summary.getTotalVacancy() - summary.getFilledVacancy());
//              }
//            }
//            /*
//             * IF PWD SEAT WAS REJECTED
//             */
//            if (Boolean.TRUE.equals(merit.getSelectedAgainstPwd()) && merit.getSelectedPwdCategoryId() != null) {
//
//                PwdTracker.PwdSummary pwdSummary = pwdTracker.getSummary(merit.getStateId(), merit.getCityId(), merit.getSelectedPwdCategoryId());
//
//                if (pwdSummary != null) {
//                    pwdSummary.setRequiredSeats(pwdSummary.getRequiredSeats() + 1);
//                    pwdSummary.setRemainingSeats(pwdSummary.getRemainingSeats() + 1);
//                }
//            }
//        }
//    }

    public void addRejectedVacancies(VacancyMatrixModel vacancyMatrixModel,List<MeritCandidateDTO> rejectedCandidates) {

        VacancyTracker vacancyTracker = vacancyMatrixModel.getVacancyTracker();

        PwdTracker pwdTracker = vacancyMatrixModel.getPwdTracker();

        if (rejectedCandidates == null || rejectedCandidates.isEmpty()) {
            return;
        }

        for (MeritCandidateDTO merit : rejectedCandidates) {

            VacancyTracker.VacancySummary summary = vacancyTracker.getSummary(merit.getStateId(), merit.getCityId(), merit.getSelectedCategoryId());

            if (summary != null) {

                /*
                 * Reopen rejected seat
                 */
                summary.setTotalVacancy(summary.getTotalVacancy() + 1);
                summary.setFilledVacancy(summary.getFilledVacancy() + 1);
                summary.getSequenceList().add(merit.getCategoryRank());

                /*
                 * Rejected converted EWS seat
                 * (UR-EWS seat)
                 */
                if (Boolean.TRUE.equals(merit.getIsEWSSeat()) && merit.getSelectedCategoryId().equals(reservationCategoryCache.getGeneralCategoryId())) {
                    summary.setEwsSeatCount(summary.getEwsSeatCount() + 1);
                }

                /*
                 * Always recalculate
                 */
                summary.setUnfilledVacancy(summary.getTotalVacancy()- summary.getFilledVacancy());
            }

            /*
             * Reopen PWD seat if rejected
             */
            if (Boolean.TRUE.equals(merit.getSelectedAgainstPwd()) && merit.getSelectedPwdCategoryId() != null) {
                PwdTracker.PwdSummary pwdSummary = pwdTracker.getSummary( merit.getStateId(), merit.getCityId(), merit.getSelectedPwdCategoryId());
                if (pwdSummary != null) {
                    pwdSummary.setRequiredSeats(pwdSummary.getRequiredSeats() + 1);
                    pwdSummary.setRemainingSeats(pwdSummary.getRemainingSeats() + 1);
                }
            }
        }
    }
    public List<MeritCandidateDTO> applyPwdReservation(List<MeritCandidateDTO> meritList,VacancyTracker vacancyTracker,PwdTracker pwdTracker ) {

        for (MeritCandidateDTO candidate : meritList) {
             // ALREADY SELECTED
            if (Boolean.TRUE.equals(candidate.getSelected())) {
                continue;
            }

            // NOT PWD
            if (!Boolean.TRUE.equals(candidate.getIsPwd())) {
                continue;
            }


            // MULTIPLE PWD TYPES
            for (UUID pwdCategoryId : candidate.getPwdCategoryIds()) {
                // PWD ALREADY SATISFIED
                PwdTracker.PwdSummary pwdSummary = pwdTracker.getSummary(candidate.getStateId(), candidate.getCityId(), pwdCategoryId);

                if (pwdSummary == null || pwdSummary.getRemainingSeats() <= 0) {
                    continue;
                }


                // CHECK VERTICAL VACANCY
                VacancyTracker.LableAndSequence lableAndSequence = vacancyTracker.fillVacancy(candidate.getStateId(), candidate.getCityId(), candidate.getCategoryId());

                /*
                 * VERTICAL VACANCY NOT AVAILABLE
                 */
                if (lableAndSequence == null) {
                    continue;
                }


                /*
                 * FILL PWD
                 */
                pwdTracker.fill(candidate.getStateId(),candidate.getCityId(),pwdCategoryId);

                /*
                 * SELECT CANDIDATE
                 */
                candidate.setSelected(true);

                candidate.setSelectedCategoryId(candidate.getCategoryId());

                candidate.setSelectionLabel(pwdCategoryCache.getCategoryCode(pwdCategoryId) +" "+lableAndSequence.getLabel());
                candidate.setCategoryRank(lableAndSequence.getSequence());

                candidate.setSelectedAgainstPwd(true);
                candidate.setSelectedPwdCategoryId(pwdCategoryId);

                /*
                 * ONE PWD SEAT ONLY
                 */
                break;
            }
        }

        return meritList;
    }


    public List<MeritCandidateDTO> calculateCombinedScores(List<MeritCandidateDTO> candidates, UUID positionId) {

        Optional<WrittenExamConfigurationEntity> examConfig = writtenExamConfigurationRepository.findByPositionId(positionId);

        /*
         * WITH WEIGHTAGE
         */
        if (examConfig.isPresent()) {
            BigDecimal totalWrittenMarks = BigDecimal.valueOf(examConfig.get().getTotalMarks());


            BigDecimal writtenWeightage = examConfig.get().getWrittenExamWeightage();

            BigDecimal interviewWeightage = examConfig.get().getInterviewWeightage();

            candidates.forEach(candidate -> {

                BigDecimal interview = candidate.getInterviewScore() != null ? candidate.getInterviewScore() : BigDecimal.ZERO;
                BigDecimal written = candidate.getWrittenScore() != null ? candidate.getWrittenScore() : BigDecimal.ZERO;

                /*
                 * WRITTEN WEIGHTED SCORE
                 */
                BigDecimal weightedWritten = written.multiply(writtenWeightage).divide(BigDecimal.valueOf(100),2,java.math.RoundingMode.HALF_UP);

                /*
                 * INTERVIEW WEIGHTED SCORE
                 */
                BigDecimal weightedInterview = interview.multiply(interviewWeightage).divide(BigDecimal.valueOf(100),2,java.math.RoundingMode.HALF_UP);

                /*
                 * FINAL COMBINED SCORE
                 */
                candidate.setExamWeightedScore(weightedWritten);
                candidate.setInterviewWeightedScore(weightedInterview);
                candidate.setInterviewScore(interview);
                candidate.setWrittenScore(written);
                candidate.setCombinedScore(weightedWritten.add(weightedInterview));
            });

        } else {

            /*
             * ONLY INTERVIEW SCORE
             */
            candidates.forEach(candidate -> {
                candidate.setInterviewScore(candidate.getInterviewScore());
                candidate.setCombinedScore( candidate.getInterviewScore() != null ? candidate.getInterviewScore() : BigDecimal.ZERO );
            });
        }

        return candidates;
    }

    private VacancyMatrixDTO buildNationalMatrix(UUID positionId, List<PositionCategoryNationalDistributionDTO> distributions){

        List<VacancyMatrixDTO.CategorySeatDTO> categorySeats = new ArrayList<>();

        List<VacancyMatrixDTO.PwdSeatDTO>pwdSeats = new ArrayList<>();

        for (PositionCategoryNationalDistributionDTO category : distributions) {

            if (Boolean.TRUE.equals(category.getIsDisability())) {

                pwdSeats.add(VacancyMatrixDTO.PwdSeatDTO.builder()
                                .categoryId(category.getReservationCategoryId())
                                .pwdCategoryId(category.getDisabilityCategoryId())
                                .seats(category.getVacancyCount())
                                .build()
                );

            } else {

                categorySeats.add(
                        VacancyMatrixDTO.CategorySeatDTO.builder()
                                .categoryId(category.getReservationCategoryId())
                                .seats(category.getVacancyCount())
                                .build()
                );
            }
        }

        return VacancyMatrixDTO.builder()
                .positionId(positionId)
                .stateId(null)
                .cityId(null)
                .categorySeats(categorySeats)
                .pwdSeats(pwdSeats)
                .build();
    }
    private VacancyMatrixDTO buildMatrix(PositionStateDistributionDTO state) {

        List<VacancyMatrixDTO.CategorySeatDTO> categorySeats = new ArrayList<>();

        for (PositionCategoryDistributionDTO category :
                state.getPositionCategoryDistributions()) {

            // Ignore PWD distributions
            if (Boolean.TRUE.equals(category.getIsDisability())) {
                continue;
            }

            categorySeats.add(
                    VacancyMatrixDTO.CategorySeatDTO.builder()
                            .categoryId(category.getReservationCategoryId())
                            .seats(category.getVacancyCount())
                            .build()
            );
        }

        return VacancyMatrixDTO.builder()
                .positionId(state.getPositionId())
                .stateId(state.getStateId())
                .cityId(state.getCityId())
                .categorySeats(categorySeats)
                .pwdSeats(Collections.emptyList())
                .build();
    }
    public VacancyMatrixModel buildVacancyMatrixModel(JobPositionsDTO jobPosition, Map<UUID, String> categoryCodeMap    ) {

        VacancyTracker tracker = new VacancyTracker();

        PwdTracker pwdTracker = new PwdTracker();

        List<VacancyMatrixDTO> vacancyMatrixList;

        /*
         * BUILD MATRIX LIST
         */
        if (Boolean.TRUE.equals(jobPosition.getIsLocationWise())) {

            vacancyMatrixList = jobPosition.getPositionStateDistributions().stream().map(this::buildMatrix).toList();

        } else {

            vacancyMatrixList = List.of(this.buildNationalMatrix(jobPosition.getId(),jobPosition.getPositionCategoryNationalDistributions())
            );
        }

        /*
         * LOAD CATEGORY VACANCIES
         */
        for (VacancyMatrixDTO matrix : vacancyMatrixList) {

            UUID stateId = matrix.getStateId();

            UUID cityId = matrix.getCityId();

            /*
             * CATEGORY SEATS
             */
            for (VacancyMatrixDTO.CategorySeatDTO seat :
                    matrix.getCategorySeats()) {

                tracker.addVacancy(stateId, cityId, seat.getCategoryId(), categoryCodeMap.get(seat.getCategoryId()),seat.getSeats());
            }

            /*
             * PWD SEATS
             */
            for (VacancyMatrixDTO.PwdSeatDTO seat : matrix.getPwdSeats()) {
                pwdTracker.addRequired(stateId, cityId, seat.getPwdCategoryId(), seat.getSeats());
            }
        }

        return VacancyMatrixModel.builder()
                .vacancyTracker(tracker)
                .pwdTracker(pwdTracker)
                .build();
    }
    public List<MeritCandidateDTO> loadConcessionCandidates(List<MeritCandidateDTO> meritCandidateDTOList, UUID positionId) {

        List<UUID> applicationsIds = meritCandidateDTOList.stream().map(MeritCandidateDTO::getApplicationId).toList();

        List<CandidateConcessionsEntity> candidateConcessionsEntities =candidateConcessionsRepository.findAllByApplicationIdIn(applicationsIds);
        Map<UUID, CandidateConcessionsEntity> candidateConcessionsMap = candidateConcessionsEntities.stream()
                .collect(Collectors.toMap(CandidateConcessionsEntity::getApplicationId, Function.identity()));

        return meritCandidateDTOList.stream().peek(entity -> {
            UUID applicationId = entity.getApplicationId();
            if (candidateConcessionsMap.containsKey(applicationId)) {
                CandidateConcessionsEntity candidateConcession = candidateConcessionsMap.get(applicationId);
                entity.setUsedAgeRelaxation(candidateConcession.getAgeConcession());
                entity.setUsedMarksRelaxation(candidateConcession.getExamConcession());
                entity.setUsedInterviewRelaxation(candidateConcession.getInterviewConcession());
            } else {
                entity.setUsedAgeRelaxation(false);
                entity.setUsedMarksRelaxation(false);
                entity.setUsedInterviewRelaxation(false);
            }
        }).toList();
    }

    public List<MeritCandidateDTO> fillReservedSeats(List<MeritCandidateDTO> meritList, VacancyTracker tracker    ) {

        for (MeritCandidateDTO candidate : meritList) {

            /*
             * ALREADY SELECTED
             */
            if (Boolean.TRUE.equals(candidate.getSelected())) {
                continue;
            }

            /*
             * CANDIDATE CATEGORY
             */
            UUID categoryId = candidate.getCategoryId();

            /*
             * FILL VACANCY
             */
            VacancyTracker.LableAndSequence lableAndSequence = tracker.fillVacancy(candidate.getStateId(), candidate.getCityId(),categoryId);

            /*
             * NO VACANCY
             */
            if (lableAndSequence == null) {
                continue;
            }

            /*
             * SELECT
             */
            candidate.setSelected(true);

            candidate.setSelectedCategoryId(categoryId);

            /*
             * SC-1
             * OBC-2
             */
            candidate.setSelectionLabel(lableAndSequence.getLabel());
            candidate.setCategoryRank(lableAndSequence.getSequence());

        }

        return meritList;
    }

    public record CategoryKey(UUID stateId,UUID cityId,UUID categoryId) {}


    // RECALCULATE CATEGORY RANK AFTER GENERAL SEAT FILLING TO ENSURE PROPER SEQUENCE FOR WAITLIST
    public List<MeritCandidateDTO> recalculateCategoryOrder(List<MeritCandidateDTO> meritList) {

        Map<CategoryKey, List<MeritCandidateDTO>> grouped =
                meritList.stream().filter(c ->Boolean.TRUE.equals(c.getSelected()))
                        .collect(Collectors.groupingBy(c -> new CategoryKey(c.getStateId(), c.getCityId(), c.getSelectedCategoryId())));

        for (List<MeritCandidateDTO> candidates : grouped.values()) {

            candidates.sort(
                    Comparator.comparing(MeritCandidateDTO::getCombinedScore, Comparator.reverseOrder())
                            .thenComparing(MeritCandidateDTO::getDob)
            );

            int rank = 1;

            for (MeritCandidateDTO candidate : candidates) {
                candidate.setCategoryRank(rank);

                /*
                 * DO NOT TOUCH
                 * selectionLabel
                 */
                rank++;
            }
        }

        return meritList;
    }


    public List<MeritCandidateDTO> generateWaitlist(List<MeritCandidateDTO> meritList, VacancyTracker tracker) {

        for (MeritCandidateDTO candidate : meritList) {

            /*
             * ALREADY SELECTED
             */
            if (Boolean.TRUE.equals( candidate.getSelected() )) {
                continue;
            }

            /*
             * FILL WAITLIST
             */
            VacancyTracker.LableAndSequence lableAndSequence = tracker.fillWaitlist(candidate.getStateId(), candidate.getCityId(), candidate.getCategoryId());

            /*
             * NO WAITLIST VACANCY
             */
            if (lableAndSequence == null) {
                continue;
            }

            /*
             * WAITLISTED
             */
            candidate.setWaitlisted(true);

            candidate.setWaitlistLabel(lableAndSequence.getLabel());
            candidate.setCategoryRank(lableAndSequence.getSequence());

            candidate.setSelectedCategoryId(candidate.getCategoryId());
        }

        return meritList;
    }
}
