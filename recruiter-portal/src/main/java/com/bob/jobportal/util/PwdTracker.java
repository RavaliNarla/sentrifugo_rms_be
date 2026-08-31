package com.bob.jobportal.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class PwdTracker {

    private final Map<PwdKey, PwdSummary> pwdMap = new HashMap<>();

    /*
     * ADD REQUIRED PWD SEATS
     */
    public void addRequired(UUID stateId, UUID cityId, UUID pwdCategoryId, int seats ) {

        PwdSummary summary = getOrCreate(stateId, cityId, pwdCategoryId);

        summary.setRequiredSeats(summary.getRequiredSeats() + seats);

        calculate(summary);
    }

    /*
     * FILL PWD SEAT
     */
    public boolean fill(UUID stateId, UUID cityId, UUID pwdCategoryId) {

        PwdSummary summary = getOrCreate(stateId, cityId, pwdCategoryId);

        /*
         * ALREADY FILLED
         */
        if (summary.getRemainingSeats() <= 0) {
            return false;
        }

        summary.setFilledSeats( summary.getFilledSeats() + 1);

        calculate(summary);

        return true;
    }

    /*
     * CALCULATE
     */
    private void calculate(PwdSummary summary ) {

        summary.setRemainingSeats(summary.getRequiredSeats()- summary.getFilledSeats());
    }

    /*
     * GET SUMMARY
     */
    public PwdSummary getSummary(UUID stateId, UUID cityId, UUID pwdCategoryId) {

        return pwdMap.get(new PwdKey(stateId,cityId,pwdCategoryId));
    }

    /*
     * GET ALL
     */
    public List<PwdSummary> getAllSummaries() {
        return List.copyOf(pwdMap.values());
    }

    /*
     * GET OR CREATE
     */
    private PwdSummary getOrCreate(UUID stateId, UUID cityId, UUID pwdCategoryId) {

        PwdKey key = new PwdKey(stateId, cityId, pwdCategoryId);

        return pwdMap.computeIfAbsent(
                key,
                k -> PwdSummary.builder()
                        .stateId(stateId)
                        .cityId(cityId)
                        .pwdCategoryId(pwdCategoryId)
                        .requiredSeats(0)
                        .filledSeats(0)
                        .remainingSeats(0)
                        .build()
        );
    }

    /*
     * KEY
     */
    public record PwdKey(UUID stateId, UUID cityId, UUID pwdCategoryId ) {}

    /*
     * SUMMARY
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PwdSummary {

        private UUID stateId;

        private UUID cityId;

        private UUID pwdCategoryId;

        private int requiredSeats;

        private int filledSeats;

        private int remainingSeats;
    }
}