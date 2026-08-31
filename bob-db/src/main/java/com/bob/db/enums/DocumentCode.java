package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DocumentCode {
    PHOTO,
    IDPROOF,
    PAYSLIP2,
    ADHAR,
    PANCR,
    PAYSLIP3,
    DIPLOMA,
    PAYSLIP1,
    RESUME,
    WORKEX,
    INTER,
    GRAD,
    POST_GRAD,
    BOARD,
    BIRTH_CERT,
    COMMUNITY_CERT,
    CASTE_CERT,
    SIGN,
    OTHERS,
    CERT;

    @JsonCreator
    public static DocumentCode fromValue(String value) {
        if (value == null || value.isBlank())throw new IllegalArgumentException("Invalid CODE.");

        for (DocumentCode status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid CODE.");
    }
}
