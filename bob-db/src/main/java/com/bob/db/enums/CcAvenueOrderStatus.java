package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CcAvenueOrderStatus {
    PENDING("PENDING"),
    SUCCESS("Success"),
    ABORTED("Aborted"),
    FAILURE("Failure"),
    INVALID("Invalid"),
    FUNDS_AUTHORIZED("Funds Authorized"),
    INITIATED("Initiated"),
    BLOCKED("Blocked"),
    CANCELLED("Cancelled"),
    UNKNOWN("Unknown"); // For any status not explicitly handled

    // Constant for use in annotations (compile-time constant)
    public static final String SUCCESS_NAME = "SUCCESS";

    private final String ccAvenueStatus;

    CcAvenueOrderStatus(String ccAvenueStatus) {
        this.ccAvenueStatus = ccAvenueStatus;
    }

    public String getCcAvenueStatus() {
        return ccAvenueStatus;
    }

    @JsonCreator
    public static CcAvenueOrderStatus fromCcAvenueStatus(String status) {
        for (CcAvenueOrderStatus orderStatus : CcAvenueOrderStatus.values()) {
            if (orderStatus.ccAvenueStatus.equalsIgnoreCase(status)) {
                return orderStatus;
            }
        }
        return UNKNOWN; // Default to UNKNOWN if status not found
    }
}
