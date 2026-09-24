package com.sentrifugo.rms.authportal.service;

import com.sentrifugo.rms.authportal.dto.ForgotPasswordRequest;
import com.sentrifugo.rms.authportal.dto.LoginRequest;
import com.sentrifugo.rms.authportal.dto.LoginResponse;
import com.sentrifugo.rms.authportal.dto.ResetPasswordRequest;
import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.service.JwtTokenService;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int OTP_TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SecurityUtils securityUtils;
    private final ObjectProvider<MailService> mailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailOrEmployeeIdIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new CommonException("Invalid username or password."));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CommonException("Invalid username or password.");
        }

        String token = jwtTokenService.issueAccessToken(user);
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInHours(jwtTokenService.getExpiryHours())
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .employeeId(user.getEmployeeId())
                        .privileges(securityUtils.getPrivileges(user.getId()))
                        .build())
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always succeed from the caller's perspective to avoid account enumeration.
        userRepository.findByEmailOrEmployeeIdIgnoreCase(request.getUsername().trim()).ifPresent(user -> {
            String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
            user.setResetOtp(otp);
            user.setResetOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
            userRepository.save(user);

            String html = "<p>Hello " + escape(user.getName()) + ",</p>"
                    + "<p>Your password reset OTP for Sagar Recruitment Hub is:</p>"
                    + "<p style='font-size:24px;font-weight:bold;letter-spacing:4px;'>" + otp + "</p>"
                    + "<p>This code expires in " + OTP_TTL_MINUTES + " minutes. If you did not request a reset, ignore this email.</p>";

            MailService mail = mailService.getIfAvailable();
            if (mail != null) {
                mail.sendHtmlEmail(user.getEmail(), "Password Reset OTP - Sagar Recruitment Hub", html);
            } else {
                log.warn("MailService unavailable — OTP for {} is {}", user.getEmail(), otp);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserEntity user = userRepository.findByEmailOrEmployeeIdIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new CommonException("Invalid OTP or username."));

        if (user.getResetOtp() == null || user.getResetOtpExpiresAt() == null
                || user.getResetOtpExpiresAt().isBefore(LocalDateTime.now())
                || !user.getResetOtp().equals(request.getOtp().trim())) {
            throw new CommonException("Invalid or expired OTP.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiresAt(null);
        userRepository.save(user);
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
