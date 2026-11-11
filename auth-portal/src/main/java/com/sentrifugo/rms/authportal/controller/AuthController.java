package com.sentrifugo.rms.authportal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentrifugo.rms.authportal.dto.ForgotPasswordRequest;
import com.sentrifugo.rms.authportal.dto.HrmsAccessResponse;
import com.sentrifugo.rms.authportal.dto.LoginRequest;
import com.sentrifugo.rms.authportal.dto.LoginResponse;
import com.sentrifugo.rms.authportal.dto.ResetPasswordRequest;
import com.sentrifugo.rms.authportal.dto.RmsAdminActiveRequest;
import com.sentrifugo.rms.authportal.dto.RmsAdminCreateRequest;
import com.sentrifugo.rms.authportal.dto.RmsAdminDTO;
import com.sentrifugo.rms.authportal.service.AuthService;
import com.sentrifugo.rms.authportal.service.HrmsLinkService;
import com.sentrifugo.rms.authportal.service.HrmsRmsAdminService;
import com.sentrifugo.rms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Authentication")
@RestController
@RequestMapping("${auth.api.base.path}")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final HrmsLinkService hrmsLinkService;
    private final HrmsRmsAdminService hrmsRmsAdminService;

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

    @Operation(summary = "List RMS Admins for HRMS Organisation Admins (includes inactive)")
    @GetMapping("/hrms/rms-admins")
    public ResponseEntity<ApiResponse<List<RmsAdminDTO>>> listRmsAdmins(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(ApiResponse.ok(hrmsRmsAdminService.list(authorization), "OK"));
    }

    @Operation(summary = "Add or reactivate an RMS Admin from HRMS Organisation Admins")
    @PostMapping("/hrms/rms-admins")
    public ResponseEntity<ApiResponse<RmsAdminDTO>> addRmsAdmin(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody RmsAdminCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(hrmsRmsAdminService.add(authorization, request), "RMS Admin added"));
    }

    @Operation(summary = "Activate or deactivate an RMS Admin (allows zero Admins)")
    @PatchMapping("/hrms/rms-admins/{id}/active")
    public ResponseEntity<ApiResponse<RmsAdminDTO>> setRmsAdminActive(
            @RequestHeader("Authorization") String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody RmsAdminActiveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                hrmsRmsAdminService.setActive(authorization, id, request), "Status updated"));
    }

    @Operation(summary = "Reset password using the emailed OTP")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully. You can now sign in."));
    }
}
