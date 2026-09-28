package com.sentrifugo.rms.db.enums;

public enum CandidateStatus {
    /** Bulk-imported without a resume - becomes ADDED once a resume is uploaded via Edit. */
    DRAFT,
    ADDED,
    SHORTLISTED,
    /** Shortlist decision = REJECT. */
    REJECTED,
    /** Shortlist decision = ON-HOLD (display label: "ON HOLD"). */
    ON_HOLD,
    /** Interview invite emailed; awaiting Accept / Decline. Slot stays booked. */
    INVITE_SENT,
    /** Candidate accepted the interview invite. */
    SCHEDULED,
    /** Candidate declined the interview invite. Slot is freed. */
    DECLINED,
    QUALIFIED,
    DISQUALIFIED,
    COMPENSATION_PENDING,
    MOVED_TO_OFFER
}
