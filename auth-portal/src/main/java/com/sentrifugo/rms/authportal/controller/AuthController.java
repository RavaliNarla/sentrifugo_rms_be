package com.sentrifugo.rms.authportal.controller;

import com.sentrifugo.rms.authportal.dto.ForgotPasswordRequest;
import com.sentrifugo.rms.authportal.dto.LoginRequest;
import com.sentrifugo.rms.authportal.dto.LoginResponse;
import com.sentrifugo.rms.authportal.dto.ResetPasswordRequest;
import com.sentrifugo.rms.authportal.service.AuthService;
import com.sentrifugo.rms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("${auth.api.base.path}")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Sign in with Employee ID or email + password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request), "Login successful"));
    }

    @Operation(summary = "Send a one-time password to the user's email for password reset")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "If an account exists for that username, an OTP has been sent to the registered email."));
    }

    @Operation(summary = "Reset password using the emailed OTP")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully. You can now sign in."));
    }
}
