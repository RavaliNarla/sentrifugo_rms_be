package com.bob.authapp.controller;

import com.bob.authapp.model.*;
import com.bob.authapp.service.CandidateAuthService;
import com.bob.commonutil.config.CaptchaConfig;
import com.bob.commonutil.service.CaptchaService;
import com.bob.commonutil.service.CloudFlareTurnstileService;
import com.bob.db.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/candidate-auth")
@Validated
@Slf4j
public class CandidateAuthController {

    @Autowired
    private CandidateAuthService candidateAuthService;

    @Autowired
    private CaptchaService captchaService;


    @PostMapping("/check-existing-email/{emailId}")
    public ResponseEntity<ApiResponse<Boolean>> checkExistingEmail(@PathVariable String emailId){
        Boolean res = candidateAuthService.checkExistingEmail(emailId);
        return  ResponseEntity.ok(ApiResponse.ok(res,""));
    }

    @PostMapping("/check-existing-phone/{phoneNo}")
    public ResponseEntity<ApiResponse<Boolean>> checkExistingPhoneNo(@PathVariable String phoneNo){
        Boolean res = candidateAuthService.checkExistingPhoneNo(phoneNo);
        return  ResponseEntity.ok(ApiResponse.ok(false,""));
    }


    /**
     * This function is used to create new row in candidate registration stages table
     * @param signupRequest
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CandidateSignupResponse>> signupCandidate(
            @Valid @RequestBody CandidateSignupRequest signupRequest) {

        boolean isVerified = captchaService.verifyCaptcha(signupRequest.getCaptcha().getCaptchaId(), signupRequest.getCaptcha().getCaptchaValue()) ;
        if (!isVerified) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Captcha verification failed"));
        }

        CandidateSignupResponse response = candidateAuthService.registerCandidate(signupRequest);
        return ResponseEntity.ok(ApiResponse.ok(response,""));
    }

    /**
     * This function validates the link clicked from the email verification and save this candidate in candidate table
     * @param token
     * @param request
     * @return
     */
    @GetMapping("/verify-token")
    public ResponseEntity<String> verifyEmail(@RequestParam @NotBlank String token,
                                              HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            log.info("Received HEAD request for email verification, likely from a security scanner. User-Agent: " + userAgent);
            return ResponseEntity.ok("Scanner request ignored");
        }
        if (userAgent != null) {
            String ua = userAgent.toLowerCase();
            if (ua.contains("safelinks") ||
                    ua.contains("microsoft") ||
                    ua.contains("proofpoint") ||
                    ua.contains("barracuda") ||
                    ua.contains("scanner")) {
                log.info("Security scanner detected based on User-Agent: " + userAgent);
                return ResponseEntity.ok("Security scanner detected");
            }
        }

        String html = candidateAuthService.verifyCandidateEmail(token);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }


    /**
     * sends otp on submitting a login request
     * @param request
     * @return
     *
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CandidateLoginPreOtpResponse>> candidateLogin(
            @Valid @RequestBody CandidateLoginRequest request
    ) throws Exception {
        CandidateLoginPreOtpResponse responseDTO = candidateAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(responseDTO, ""));
    }

    @PostMapping("/edit-mobile-no/{newMobileNo}")
    public ResponseEntity<ApiResponse<CandidateLoginPreOtpResponse>> editMobileNo(
            @Valid @RequestBody CandidateLoginRequest request, @PathVariable String newMobileNo
    ) throws Exception {
        CandidateLoginPreOtpResponse responseDTO = candidateAuthService.editMobileNo(request,newMobileNo);
        return ResponseEntity.ok(ApiResponse.ok(responseDTO, ""));
    }

    /**
     * both first time & future otps use this api to validate. first time it will save candidate to candidates table.
     * @param request
     * @param httpRequest
     * @param httpResponse
     * @return
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<CandidateLoginResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request,
                                                     HttpServletRequest httpRequest,
                                                     HttpServletResponse httpResponse) {
        CandidateLoginResponse token = candidateAuthService.verifyOtp(request.getEmail(), request.getOtp(), httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.ok(token, "OTP verified successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
                                    HttpServletResponse httpResponse) {

        String message = candidateAuthService.logout(refreshToken,httpResponse);
        ApiResponse<String> response = new ApiResponse<>(true, message, null);
        return ResponseEntity.ok(response);
    }

    record ResendOtpResponse(LocalDateTime otpExpiresAt) {}
    @PostMapping("/resend-otp")
    public ResponseEntity<Object> resendOtp(@Valid @RequestBody String email) throws Exception {
        LocalDateTime otpExpiry = candidateAuthService.resendOtp(email);
        ResendOtpResponse response = new ResendOtpResponse(otpExpiry);

        return ResponseEntity.ok(ApiResponse.ok(response, "OTP resent successfully"));
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<String>> refresh(@CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
                                                  HttpServletResponse httpResponse) {
        String message = candidateAuthService.refreshToken(refreshToken,httpResponse);
        ApiResponse<String> response = new ApiResponse<>(true, message, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Boolean>> forgotPassword(@RequestParam @NotBlank @Email String email) throws Exception {
        String message = candidateAuthService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.ok(true, message));
    }
    @GetMapping("/password-otp-verify")
    public ResponseEntity<ApiResponse<Boolean>> passwordOtpVerify(@RequestParam @NotBlank @Email String email, @RequestParam @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otp) {
        String message = candidateAuthService.passwordOtpVerify(email,otp);
        return ResponseEntity.ok(ApiResponse.ok(true, message));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Boolean>> resetPasswordForm(@Valid @RequestBody ResetPasswordRequest request) {
        String message = candidateAuthService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(message));
    }
}
