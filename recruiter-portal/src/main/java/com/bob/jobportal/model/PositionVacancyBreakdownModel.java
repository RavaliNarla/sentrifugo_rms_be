package com.bob.jobportal.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PositionVacancyBreakdownModel {

    // ── Position identifiers ──────────────────────────────────────────────────
    private UUID positionId;
    private UUID masterPositionId;

    // ── Popup header fields ───────────────────────────────────────────────────
    private UUID deptId;
    private UUID employmentType;
    /** Populated only when position is Contract type. */
    private Integer contractYears;
    private Integer eligibilityAgeMin;
    private Integer eligibilityAgeMax;

    // ── Experience ────────────────────────────────────────────────────────────
    /** Total mandatory experience in months (flat; UI converts to years+months). */
    private Integer mandatoryExperienceMonths;
    /** When true, experience differs per education level — use mandatoryExpMonthsEduWise. */
    private Boolean isMandatoryExpMonthsEduWise;
    /** { qualificationId -> months }. Populated when isMandatoryExpMonthsEduWise = true. */
    private JsonNode mandatoryExpMonthsEduWise;

    // ── Vacancy totals (position-level) ──────────────────────────────────────
    private Boolean isLocationWise;
    private Integer totalVacancies;
    private Integer remainingTotalVacancies;
    private Integer onboardedCount;
    private Integer offersSent;
    private Integer offersAccepted;

    /** Populated only when isLocationWise = false */
    private List<NationalCategoryBreakdown> nationalBreakdown;

    /** Populated only when isLocationWise = true */
    private List<StateDistributionBreakdown> stateBreakdown;

    @Data
    @Builder
    public static class NationalCategoryBreakdown {
        private UUID reservationCategoryId;
        private UUID disabilityCategoryId;
        private Boolean isDisability;
        private Integer vacancyCount;
        private Integer remainingVacancyCount;
        private Integer onboardedCount;
        private Integer offersSent;
        private Integer offersAccepted;
    }

    @Data
    @Builder
    public static class StateDistributionBreakdown {
        private UUID stateId;
        private UUID cityId;
        private Integer totalVacancies;
        private Integer remainingTotalVacancies;
        private Integer onboardedCount;
        private Integer offersSent;
        private Integer offersAccepted;
        private List<CategoryBreakdown> categories;
    }

    @Data
    @Builder
    public static class CategoryBreakdown {
        private UUID reservationCategoryId;
        private UUID disabilityCategoryId;
        private Boolean isDisability;
        private Integer vacancyCount;
        private Integer remainingVacancyCount;
        private Integer onboardedCount;
        private Integer offersSent;
        private Integer offersAccepted;
    }

    /** onboardedCount = original - remaining; 0 if remaining is null (nothing deducted yet). */
    public static int calcOnboarded(Integer original, Integer remaining) {
        if (remaining == null) return 0;
        return (original != null ? original : 0) - remaining;
    }
}
