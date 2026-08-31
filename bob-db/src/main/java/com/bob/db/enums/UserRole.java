package com.bob.db.enums;

public enum UserRole {
    ADMIN("Admin"),
//    MANAGER("Manager"),
    RECRUITER("Recruiter"),
//    INTERVIEWER("Interviewer"),
    COMMITTEE_MEMBER("Committee_Member"),
    ZONAL_HR("Zonal_HR");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (UserRole role : UserRole.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.value;
    }
}