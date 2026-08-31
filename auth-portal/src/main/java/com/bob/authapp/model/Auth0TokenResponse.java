package com.bob.authapp.model;

import lombok.Data;

@Data
public class Auth0TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String idToken;
    private Boolean mfaRequired;
    private String mfaToken;
    private Integer expiresIn;
    private String tokenType;
    // getters/setters
}