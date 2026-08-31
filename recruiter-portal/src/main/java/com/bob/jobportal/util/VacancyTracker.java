package com.bob.jobportal.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Data
@Slf4j
public class VacancyTracker {

    /*
     * STATE + CITY + CATEGORY
     */
    private final Map<VacancyKey, VacancySummary> vacancyMap = new HashMap<>();

    /*
     * ADD VACANCY
     */
    public void addVacancy(UUID stateId, UUID cityId, UUID categoryId, String categoryCode, int seats) {
        VacancySummary summary = getOrCreate(stateId, cityId, categoryId, categoryCode);
        summary.setTotalVacancy(summary.getTotalVacancy() + seats);
        summary.setWaitlistVacancy((int) Math.ceil(summary.getTotalVacancy() * 0.25));
        calculate(summary);
    }

    public String fillOccupyVacancy( UUID stateId, UUID cityId, UUID categoryId,Integer categoryRank, Boolean isEwsSeat,ReservationCategoryCache reservationCategoryCache ) {

        VacancySummary summary =  findMatchingVacancy( stateId, cityId, categoryId);

        /*
         * VACANCY NOT FOUND
         */
        if (summary == null) {
            return null;
        }

        /*
         * NO VACANCY AVAILABLE
         */
        if (summary.getUnfilledVacancy() <= 0) {
            return null;
        }

        if(isEwsSeat && categoryId.equals(reservationCategoryCache.getGeneralCategoryId())){
            VacancySummary ewsSummary =  findMatchingVacancy( stateId, cityId, reservationCategoryCache.getEwsCategoryId());
            summary.setEwsSeatCount(summary.getEwsSeatCount() +1);
            summary.setTotalVacancy(summary.getTotalVacancy() + 1);
            calculate(summary);
            ewsSummary.setTotalVacancy(ewsSummary.getTotalVacancy() -1);
            calculate(ewsSummary);
            log.info(
                    "BEFORE OCCUPY EWS->GEN GEN_TOTAL={} GEN_FILLED={} GEN_UNFILLED={} EWS_TOTAL={} EWS_FILLED={} EWS_UNFILLED={}",
                    summary.getTotalVacancy(),
                    summary.getFilledVacancy(),
                    summary.getUnfilledVacancy(),
                    ewsSummary.getTotalVacancy(),
                    ewsSummary.getFilledVacancy(),
                    ewsSummary.getUnfilledVacancy()
            );
        }


        /*
         * INCREMENT FILLED
         */
        summary.setFilledVacancy(summary.getFilledVacancy() + 1);
        summary.getSequenceList().add(categoryRank);
        calculate(summary);


        /*
         * LABEL
         * GEN-1
         * SC-2
         */
        int sequence = summary.getNextSequence() + 1;

        return summary.getCategoryCode() + "-" + sequence;
    }

    /*
     * FILL VACANCY
     */
    public LableAndSequence fillVacancy( UUID stateId, UUID cityId, UUID categoryId) {

        VacancySummary summary =  findMatchingVacancy( stateId, cityId, categoryId);

        /*
         * VACANCY NOT FOUND
         */
        if (summary == null) {
            return null;
        }

        /*
         * NO VACANCY AVAILABLE
         */
        if (summary.getUnfilledVacancy() <= 0) {
            return null;
        }
        log.info(
                "FILL VACANCY START category={} total={} filled={} unfilled={}",
                summary.getCategoryCode(),
                summary.getTotalVacancy(),
                summary.getFilledVacancy(),
                summary.getUnfilledVacancy()
        );
        /*
         * INCREMENT FILLED
         */
        summary.setFilledVacancy(summary.getFilledVacancy() + 1);

        calculate(summary);
        log.info(
                "FILL VACANCY END category={} total={} filled={} unfilled={}",
                summary.getCategoryCode(),
                summary.getTotalVacancy(),
                summary.getFilledVacancy(),
                summary.getUnfilledVacancy()
        );

        /*
         * LABEL
         * GEN-1
         * SC-2
         */
        int sequence = summary.getNextSequence() + 1;

        while (summary.getSequenceList().contains(sequence)) {
            sequence++;
        }



        summary.getSequenceList().add(sequence);
        summary.setNextSequence(sequence);

        if (summary.getEwsSeatCount() != null
                && summary.getEwsSeatCount() > 0
                && summary.getUnfilledVacancy() < summary.getEwsSeatCount()) {

            return LableAndSequence.builder()
                    .label(summary.getCategoryCode() + "-EWS-" + sequence)
                    .sequence(sequence)
                    .isEWSSeat(true)
                    .build();
        }

        return  LableAndSequence.builder()
                .label(summary.getCategoryCode() + "-" + sequence)
                .sequence(sequence)
                .isEWSSeat(false)
                .build();
    }

    public int getRemainingVacancy(UUID stateId, UUID cityId, UUID categoryId) {

        VacancySummary summary =
                findMatchingVacancy(stateId, cityId, categoryId);

        return summary == null ? 0 : summary.getUnfilledVacancy();
    }

    public void transferVacancy(
            UUID stateId,
            UUID cityId,
            UUID fromCategoryId,
            UUID toCategoryId,
            int count) {

        VacancySummary fromSummary = findMatchingVacancy(stateId, cityId, fromCategoryId);

        VacancySummary toSummary = findMatchingVacancy(stateId, cityId, toCategoryId);

        if (fromSummary == null || toSummary == null || count <= 0) {
            return;
        }

        int transferable = Math.min(count, fromSummary.getUnfilledVacancy());

        if (transferable <= 0) {
            return;
        }

        /*
         * Reduce source vacancy
         */
        fromSummary.setTotalVacancy(fromSummary.getTotalVacancy() - transferable);

        calculate(fromSummary);

        /*
         * Increase target vacancy
         */
        toSummary.setTotalVacancy( toSummary.getTotalVacancy() + transferable);
        toSummary.setEwsSeatCount(transferable);
        toSummary.setUnfilledVacancy( toSummary.getUnfilledVacancy() + transferable);
        calculate(toSummary);
    }

    public void transferAllRemainingVacancies(UUID fromCategoryId, UUID toCategoryId) {

        List<VacancySummary> summaries =
                vacancyMap.values()
                        .stream()
                        .filter(v -> v.getCategoryId().equals(fromCategoryId))
                        .toList();

        for (VacancySummary summary : summaries) {

            int remaining = summary.getUnfilledVacancy();

            if (remaining > 0) {

                VacancySummary gen = findMatchingVacancy(summary.getStateId(), summary.getCityId(), toCategoryId);

                log.info(
                        "BEFORE TRANSFER state={} city={} GEN_TOTAL={} GEN_FILLED={} GEN_UNFILLED={} EWS_TOTAL={} EWS_FILLED={} EWS_UNFILLED={} TRANSFER={}",
                        summary.getStateId(),
                        summary.getCityId(),
                        gen != null ? gen.getTotalVacancy() : 0,
                        gen != null ? gen.getFilledVacancy() : 0,
                        gen != null ? gen.getUnfilledVacancy() : 0,
                        summary.getTotalVacancy(),
                        summary.getFilledVacancy(),
                        summary.getUnfilledVacancy(),
                        remaining
                );

                transferVacancy(summary.getStateId(), summary.getCityId(), fromCategoryId, toCategoryId, remaining);

                gen = findMatchingVacancy(
                        summary.getStateId(),
                        summary.getCityId(),
                        toCategoryId);

                VacancySummary ews =
                        findMatchingVacancy(
                                summary.getStateId(),
                                summary.getCityId(),
                                fromCategoryId);

                log.info(
                        "AFTER TRANSFER state={} city={} GEN_TOTAL={} GEN_FILLED={} GEN_UNFILLED={} EWS_TOTAL={} EWS_FILLED={} EWS_UNFILLED={}",
                        summary.getStateId(),
                        summary.getCityId(),
                        gen != null ? gen.getTotalVacancy() : 0,
                        gen != null ? gen.getFilledVacancy() : 0,
                        gen != null ? gen.getUnfilledVacancy() : 0,
                        ews != null ? ews.getTotalVacancy() : 0,
                        ews != null ? ews.getFilledVacancy() : 0,
                        ews != null ? ews.getUnfilledVacancy() : 0
                );
            }
        }
    }
    public VacancySummary findMatchingVacancy(UUID stateId, UUID cityId, UUID categoryId) {

        /*
         * EXACT MATCH
         * State + City + Category
         */
        VacancySummary summary = vacancyMap.get(new VacancyKey(stateId, cityId, categoryId));

        if (summary != null) {
            return summary;
        }

        /*
         * STATE LEVEL MATCH
         * State + NULL + Category
        */
        summary = vacancyMap.get(new VacancyKey(stateId, null, categoryId));

        if (summary != null) {
            return summary;
        }

            /*
            * CITY LEVEL MATCH
            * NULL + City + Category
            */
        return vacancyMap.get(new VacancyKey(null, null, categoryId));
    }
    public boolean hasVacancy(UUID stateId, UUID cityId, UUID categoryId) {

        VacancySummary summary = findMatchingVacancy( stateId, cityId, categoryId);

        return summary != null && summary.getUnfilledVacancy() > 0;
    }

    public LableAndSequence fillWaitlist(UUID stateId, UUID cityId, UUID categoryId) {

        VacancySummary summary = findMatchingVacancy(stateId, cityId, categoryId);

        if (summary == null) {
            return null;
        }

        /*
         * NO WAITLIST VACANCY
         */
        if (summary.getFilledWaitlist() >= summary.getWaitlistVacancy()) {
            return null;
        }

        /*
         * NEXT RANK NUMBER
         */
        Integer sequence = summary.getNextSequence() + 1;

        while (summary.getSequenceList().contains(sequence)) {
            sequence++;
        }

        summary.getSequenceList().add(sequence);
        summary.setNextSequence(sequence);

        /*
         * Increment waitlist count
         */
        summary.setFilledWaitlist(summary.getFilledWaitlist() + 1);

        return LableAndSequence.builder()
                .label("WL-" + summary.getCategoryCode() + "-" + summary.getFilledWaitlist())
                .sequence(sequence)
                .build();
    }

    public VacancySummary getSummary(UUID stateId, UUID cityId, UUID categoryId) {

        return findMatchingVacancy(stateId, cityId, categoryId);
    }

    /*
     * REMAINING
     */
    private static void calculate(VacancySummary summary) {
        summary.setUnfilledVacancy(summary.getTotalVacancy() - summary.getFilledVacancy());
    }

    /*
     * GET OR CREATE
     */
    private VacancySummary getOrCreate(UUID stateId, UUID cityId, UUID categoryId, String categoryCode) {

        VacancyKey key = new VacancyKey(stateId, cityId, categoryId);

        return vacancyMap.computeIfAbsent(
                key,
                k -> VacancySummary.builder()
                        .stateId(stateId)
                        .cityId(cityId)
                        .categoryId(categoryId)
                        .categoryCode(categoryCode)
                        .totalVacancy(0)
                        .filledVacancy(0)
                        .unfilledVacancy(0)
                        .waitlistVacancy(0)
                        .filledWaitlist(0)
                        .nextSequence(0)
                        .ewsSeatCount(0)
                        .sequenceList(new ArrayList<>())
                        .build()
        );
    }

    /*
     * KEY
     */
    public record VacancyKey(UUID stateId, UUID cityId, UUID categoryId) {}

    /*
     * SUMMARY
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VacancySummary {

        private UUID stateId;

        private UUID cityId;

        private UUID categoryId;

        /*
         * GEN / SC / ST / OBC
         */
        private String categoryCode;

        private Integer totalVacancy;

        private Integer filledVacancy;

        private Integer waitlistVacancy;

        private Integer filledWaitlist;

        private Integer unfilledVacancy;

        private Integer nextSequence;

        private List<Integer> sequenceList;

        private Integer ewsSeatCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LableAndSequence {
        private String label;
        private Integer sequence;
        private Boolean isEWSSeat;
    }

}