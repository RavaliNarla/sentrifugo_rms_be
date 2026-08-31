package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RequisitionStatus {
    NEW,
    L1_PENDING,
//    L1_APPROVED,
    APPROVED,
    L1_REJECTED,
    L2_REJECTED,
    L2_PENDING,
    CLOSED;

    @JsonCreator
    public static RequisitionStatus fromValue(String value) {
        if (value == null || value.isBlank()) return NEW;

        for (RequisitionStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Requisition status.");
    }
}
