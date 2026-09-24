package com.sentrifugo.rms.common.util;

public class AppConstants {

    private AppConstants() {
    }

    public static final String[] PUBLIC_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/v1/public/**",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/actuator/health"
    };

    public static final String GENERAL_CATEGORY_HEADER = "Authorization";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_RECRUITER = "RECRUITER";
    public static final String ROLE_COMMITTEE_MEMBER = "COMMITTEE_MEMBER";

    /** Default password applied when backfilling existing users (change via Forgot Password). */
    public static final String DEFAULT_USER_PASSWORD = "Sagarsoft@12345";
}
