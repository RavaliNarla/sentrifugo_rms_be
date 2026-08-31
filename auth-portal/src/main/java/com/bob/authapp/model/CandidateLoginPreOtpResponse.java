package com.bob.authapp.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CandidateLoginPreOtpResponse {
    boolean isLoginSuccess;
    boolean isPasswordExpired;
    boolean canEditMobile;
    String mobileNumber;
    String responseMessage;
    LocalDateTime otpExpiry;
}
