package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SenderTypeEnum {

    CANDIDATE,
    RECRUITER;

    @JsonCreator
    public static SenderTypeEnum fromValue(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid Sender Type.");
        }

        for (SenderTypeEnum status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid Sender Type.");
    }
}