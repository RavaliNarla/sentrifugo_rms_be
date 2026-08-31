package com.bob.authapp.controller;

import com.bob.authapp.model.RecruiterLoginRequest;
import com.bob.authapp.model.RecruiterRegisterRequest;

import com.bob.authapp.service.RecruiterAuthService;
import com.bob.authapp.utils.AppConstants;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/recruiter-auth")
@Validated
public class RecruiterAuthController {
    @Autowired
    private RecruiterAuthService recruiterService;

    @PostMapping("/recruiter-register")
    public ResponseEntity<?> registerRecruiter(@Valid @RequestBody RecruiterRegisterRequest req) {
        try {
            return ResponseEntity.ok(recruiterService.registerRecruiter(req));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(409).body(Map.of(
                   AppConstants.ERROR, "Email already registered"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of(
                   AppConstants.ERROR, e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                   AppConstants.ERROR, "Registration failed",
                    AppConstants.DETAILS, e.getMessage()
            ));
        }
    }

    @PostMapping("/recruiter-login")
    public ResponseEntity<?> loginRecruiter(@Valid @RequestBody RecruiterLoginRequest req,
                                            HttpServletResponse response) {
        try {
            return recruiterService.loginRecruiter(req, response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                   AppConstants.ERROR, "Login failed",
                    "error_description", e.getMessage()
            ));
        }
    }

    @PostMapping("/recruiter-resend-verification")
    public ResponseEntity<?> resendVerification(@RequestParam String userId) {
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "user_id is required"));
            }

            return ResponseEntity.ok(recruiterService.resendVerificationEmail(userId));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                   AppConstants.ERROR, "Failed to resend verification email",
                    AppConstants.DETAILS, e.getMessage()
            ));
        }
    }

    @PostMapping("/recruiter-refresh-token")
    public ResponseEntity<Map<String,String>> refreshRecruiterToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        try {
            if (refreshToken == null) {
                return ResponseEntity.status(401).body(Map.of("error", "No refresh token"));
            }

            return recruiterService.refreshToken(refreshToken, response);

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                   AppConstants.ERROR, "Failed to refresh token",
                    AppConstants.DETAILS, e.getMessage()
            ));
        }
    }

    @PostMapping("/recruiter-forgot-password")
    public ResponseEntity<Map<String,String>> forgotPassword(@RequestParam @NotBlank @Email String email) {


        try {
           recruiterService.sendRecruiterPasswordReset(email);
            return ResponseEntity.ok(Map.of("message", "Password reset email sent."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                   AppConstants.ERROR, "Failed to send reset email",
                    AppConstants.DETAILS, e.getMessage()
            ));
        }
    }
}
