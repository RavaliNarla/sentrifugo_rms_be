package com.sentrifugo.rms.authportal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentrifugo.rms.authportal.dto.ForgotPasswordRequest;
import com.sentrifugo.rms.authportal.dto.HrmsAccessResponse;
import com.sentrifugo.rms.authportal.dto.LoginRequest;
import com.sentrifugo.rms.authportal.dto.LoginResponse;
import com.sentrifugo.rms.authportal.dto.ResetPasswordRequest;
import com.sentrifugo.rms.authportal.service.AuthService;
import com.sentrifugo.rms.authportal.service.HrmsLinkService;
import com.sentrifugo.rms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("${auth.api.base.path}")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final HrmsLinkService hrmsLinkService;

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

    @Operation(summary = "Whether the signed-in HRMS user is listed in RMS")
    @GetMapping("/hrms/access")
    public ResponseEntity<ApiResponse<HrmsAccessResponse>> hrmsAccess(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(ApiResponse.ok(hrmsLinkService.access(authorization), "OK"));
    }

    @Operation(summary = "Exchange a live HRMS session for an RMS session")
    @PostMapping("/hrms/enter")
    public ResponseEntity<ApiResponse<LoginResponse>> hrmsEnter(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(ApiResponse.ok(hrmsLinkService.enter(authorization), "Login successful"));
    }

    @Operation(summary = "Search HRMS employees for the Add User dialog")
    @GetMapping("/hrms/employees")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<JsonNode>> hrmsEmployees(
            @RequestParam String search,
            @RequestHeader("X-Hrms-Token") String hrmsToken) {
        return ResponseEntity.ok(ApiResponse.ok(hrmsLinkService.searchEmployees(hrmsToken, search), "OK"));
    }

    @Operation(summary = "Reset password using the emailed OTP")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully. You can now sign in."));
    }
}
