package com.bob.commonutil.enums;

public enum AvailableScreens {
    ADMIN("Admin"),
    VERIFICATION("Verification"),

    JOB_POSTINGS("JobPostings"),

    CANDIDATE_POOL("Candidate Pool"),
    SCHEDULE_POOL("Schedule Pool"),
    INTERVIEW_POOL("Interview Pool"),
    OFFER_POOL("Offer Pool"),
    COMPENSATION_POOL("Compensation Pool"),
    MESSAGES("Messages"),
    EXAM_CONFIGURATION("ExaminationCutoffConfiguration"),
    COMMITTEE_MANAGEMENT("Committee Management"),

    INTERVIEW_SCORE("Interview"),
    L1_APPROVAL("L1 Approval"),
    L2_APPROVAL("L2 Approval"),
    VIEW_POSITION("View Position"),
    DASHBOARD("Dashboard");

    private final String displayName;

    AvailableScreens(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
