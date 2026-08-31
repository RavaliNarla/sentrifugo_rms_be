package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OfferApprovalStatus {
    L1_PENDING,
    //    L1_APPROVED,
    APPROVED,
    L1_REJECTED,
    L2_REJECTED,
    L2_PENDING,
    CLOSED;

    @JsonCreator
    public static OfferApprovalStatus fromValue(String value) {
        if (value == null || value.isBlank()) return L1_PENDING;

        for (OfferApprovalStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Requisition status.");
    }
}
