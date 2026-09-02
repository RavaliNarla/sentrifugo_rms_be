package com.sentrifugo.rms.db.enums;

public enum UserRole {
    ADMIN("Admin"),
    RECRUITER("Recruiter"),
    COMMITTEE_MEMBER("Committee_Member");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromValue(String value) {
        for (UserRole role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
