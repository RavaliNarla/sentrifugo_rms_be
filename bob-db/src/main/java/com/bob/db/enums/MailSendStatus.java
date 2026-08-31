package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MailSendStatus {
    PENDING,
    SENT,
    FAILED;

    @JsonCreator
    public static MailSendStatus fromValue(String value) {
        if (value == null || value.isBlank()){
            throw new IllegalArgumentException("Invalid Scheduling Status.");
        }

        for (MailSendStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Scheduling Status.");
    }
}
