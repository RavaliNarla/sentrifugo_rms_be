package com.sentrifugo.rms.db.enums;

public enum CandidateStatus {
    /** Bulk-imported without a resume - becomes ADDED once a resume is uploaded via Edit. (Formerly DRAFT.) */
    RESUME_PENDING,
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
    /** Compensation details saved; eligible to move to Offer Pool. */
    COMPENSATION_SUBMITTED,
    MOVED_TO_OFFER
}
