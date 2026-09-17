package com.sentrifugo.rms.db.enums;

public enum OfferStatus {
    /** Drafted (or edited/regenerated after a rejection) - not yet submitted for approval, not sent. */
    GENERATED,
    /** Offer approval workflow (Section 12/16 of the requirements doc) - retained approval flow #2. */
    L1_PENDING,
    L2_PENDING,
    L1_REJECTED,
    L2_REJECTED,
    /** L2-approved and automatically emailed to the candidate. */
    SENT,
    ACCEPTED,
    REJECTED,
    EXPIRED
}
