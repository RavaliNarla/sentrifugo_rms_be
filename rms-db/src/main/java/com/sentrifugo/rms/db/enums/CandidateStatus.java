package com.sentrifugo.rms.db.enums;

public enum CandidateStatus {
    ADDED,
    SHORTLISTED,
    /** Shortlist decision = No (FRS section 8: Yes / No / On Hold). */
    NOT_SHORTLISTED,
    /** Shortlist decision = On Hold - recruiter can revisit later. */
    ON_HOLD,
    SCHEDULED,
    QUALIFIED,
    DISQUALIFIED,
    COMPENSATION_PENDING,
    MOVED_TO_OFFER
}
