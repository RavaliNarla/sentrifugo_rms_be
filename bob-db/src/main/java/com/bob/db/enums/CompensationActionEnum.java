package com.bob.db.enums;


import com.fasterxml.jackson.annotation.JsonCreator;

public enum CompensationActionEnum {
    SUBMIT,
    APPROVE,
    REJECT,
    RENEGOTIATE;

    @JsonCreator
    public static CompensationActionEnum fromValue(String value) {
        if (value == null || value.isBlank()){
            throw new IllegalArgumentException("Invalid Scheduling Status.");
        }

        for (CompensationActionEnum status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Scheduling Status.");
    }
}