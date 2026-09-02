package com.sentrifugo.rms.common.util;

public class AppConstants {

    private AppConstants() {
    }

    public static final String[] PUBLIC_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/v1/public/**",
            "/actuator/health"
    };

    public static final String GENERAL_CATEGORY_HEADER = "Authorization";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_RECRUITER = "RECRUITER";
    public static final String ROLE_COMMITTEE_MEMBER = "COMMITTEE_MEMBER";
}
