package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationFeeCategoryCode {

    SC("SC"),
    ST("ST"),
    PWD("PWD"),
    ESM("ESM"),
    WOMEN("FEMALE"),
    GEN("GEN"),
    EWS("EWS"),
    OBC("OBC"),
    DEFAULT("DEFAULT");

    private final String code;

    ApplicationFeeCategoryCode(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ApplicationFeeCategoryCode fromValue(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid Application Fee Category.");
        }

        for (ApplicationFeeCategoryCode category : values()) {
            if (category.code.equalsIgnoreCase(value) ||
                    category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }

        throw new IllegalArgumentException("Invalid Application Fee Category.");
    }
}