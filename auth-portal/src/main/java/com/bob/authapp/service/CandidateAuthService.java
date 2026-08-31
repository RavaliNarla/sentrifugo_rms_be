package com.bob.authapp.service;

import com.bob.authapp.controller.CandidateAuthController;
import com.bob.authapp.model.*;
import com.bob.authapp.utils.AppConstants;
import com.bob.commonutil.enums.SmsTemplateType;
import com.bob.commonutil.exception.AuthenticationException;
import com.bob.commonutil.exception.DuplicateException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.CredModel;
import com.bob.commonutil.model.SmsRequest;
import com.bob.commonutil.service.SmsService;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.JwtUtil;
import com.bob.commonutil.util.RSADecryptionUtil;
import com.bob.db.entity.CandidateRegistrationStagesEntity;
import com.bob.db.entity.CandidatesEntity;
import com.bob.db.repository.CandidateRegistrationStagesRepository;
import com.bob.db.repository.CandidatesRepository;
import com.bob.db.util.DBConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static com.bob.authapp.utils.AppConstants.*;

@Service
@Slf4j
public class CandidateAuthService {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private CandidateRegistrationStagesRepository candidateRegistrationStagesRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MailService mailService;

    @Autowired
    private RSADecryptionUtil rsaDecryptionUtil;

    @Autowired
    private CandidateSessionManagementService candidateSessionManagementService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Value("${app.otp.expiration-minutes}")
    private String otpExpirationMinutes;

    @Value("${ui.candidate.url}")
    private String uiCandidateUrl;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    @Transactional
    public CandidateSignupResponse registerCandidate(CandidateSignupRequest req) {

        CredModel credentials = rsaDecryptionUtil.decryptToken(req.getCredentials());

        if (credentials == null ) {
            throw new IllegalArgumentException("Invalid credentials format.");
        }

        String email = credentials.getEmail();
        String password = credentials.getPassword();

        if(this.checkExistingEmail(email)){
            throw new DuplicateException("Email Id already registered.");
        }

        if( this.checkExistingPhoneNo(req.getMobileNumber())){
            throw new DuplicateException("Mobile number already registered.");
        }

        // Generate token
        String verificationToken = UUID.randomUUID().toString();

        CandidateRegistrationStagesEntity candidate =
                CandidateRegistrationStagesEntity.builder()
                        .fullName(req.getFullName())
                        .mobileNumber(req.getMobileNumber().toString())
                        .email(email)
                        .passwordHash(password)
                        .dateOfBirth(req.getDateOfBirth())
                        .isEmailVerified(false)
                        .isOtpVerified(false)
                        .verificationToken(verificationToken)
                        .tokenExpiry(LocalDateTime.now().plusDays(1))
                        .privacyNoticeAccepted(req.getPrivacyNoticeAccepted())
                        .build();

        UUID id = candidateRegistrationStagesRepository.save(candidate).getId();

        mailService.sendVerificationEmail(email, candidate.getFullName(), verificationToken);

        log.info("Candidate Registration Successful");
        return CandidateSignupResponse.builder()
                .candidateId(id)
                .message("Registration successful. Verification email sent.")
                .build();
    }


    @Transactional
    public String verifyCandidateEmail(String token) {

        Optional<CandidateRegistrationStagesEntity> optionalCandidate =
                candidateRegistrationStagesRepository.findByVerificationToken(token);

        Context context = new Context();
        context.setVariable(AppConstants.UI_CANDIDATE_URL, uiCandidateUrl);
        // CASE 1 → INVALID TOKEN
        if (optionalCandidate.isEmpty()) {
            log.info("Invalid Token");
            context.setVariable(EMAIL_STATUS, EMAIL_STATUS_FAILED);   // show FAILED section
            return templateEngine.process(VERIFICATION_RESULT_TEMPLATE, context);
        }

        CandidateRegistrationStagesEntity candidate_registration_stages_entity = optionalCandidate.get();

        // CASE 2 → TOKEN EXPIRED
        if (candidate_registration_stages_entity.getTokenExpiry().isBefore(LocalDateTime.now())) {
            log.info("Token Expired");
            context.setVariable(AppConstants.EMAIL_STATUS, EMAIL_STATUS_EXPIRED);  // show EXPIRED section
            context.setVariable(AppConstants.UI_CANDIDATE_URL, uiCandidateUrl);
            return templateEngine.process(VERIFICATION_RESULT_TEMPLATE, context);
        }

        // CASE 3 → VALID TOKEN → VERIFY EMAIL, delete fake emails
        candidate_registration_stages_entity.setIsEmailVerified(true);
        candidate_registration_stages_entity.setVerificationToken(null);
        candidate_registration_stages_entity.setTokenExpiry(null);

        String verifiedEmail = candidate_registration_stages_entity.getEmail();
        candidateRegistrationStagesRepository.deleteAllByEmailIgnoreCaseAndVerificationTokenNot(verifiedEmail,token);
        candidateRegistrationStagesRepository.save(candidate_registration_stages_entity);
        log.info("Candidate Verification Successful");

        context.setVariable(AppConstants.EMAIL_STATUS, EMAIL_STATUS_SUCCESS);
        return templateEngine.process(VERIFICATION_RESULT_TEMPLATE, context);
    }


    @Transactional(dontRollbackOn = AuthenticationException.class)
    public CandidateLoginPreOtpResponse login(CandidateLoginRequest request) throws Exception {

        CredModel credentials = rsaDecryptionUtil.decryptToken(request.getCredentials());

        if (credentials == null ) {
            throw new AuthenticationException("Invalid credentials format.");
        }
        Optional<CandidatesEntity> optionalCandidate = candidatesRepository.findByEmailIgnoreCase(credentials.getEmail());
        if(optionalCandidate.isPresent()){
            // ----------------- Already verified candidate -----------------
            CandidatesEntity candidate = optionalCandidate.get();

            // ----------------- Account Lock Check -----------------
            if (Boolean.TRUE.equals(candidate.getCandidateTempLock())) {
                throw new AuthenticationException("Account locked. Reason: " + candidate.getCandidateReasonLocked());
            }

            if(candidate.getPasswordExpiryDate().isBefore(LocalDateTime.now())){
                return CandidateLoginPreOtpResponse.builder()
                        .isLoginSuccess(false)
                        .isPasswordExpired(true)
                        .responseMessage("Password has expired")
                        .build();
            }

            if (candidate.getPasswordHash().equals(credentials.getPassword())) {
                // ----------------- PASSWORD IS CORRECT -----------------
                candidate.setCandidateReasonLocked(String.valueOf(MAX_LOGIN_ATTEMPTS));
                candidate.setCandidateTempLock(false);

                String otp = generateRandomOtp();
                LocalDateTime otpExpiry =LocalDateTime.now().plusMinutes(Integer.parseInt(otpExpirationMinutes));

                candidate.setOtpCode(otp);
                candidate.setOtpExpiry(otpExpiry);
                candidate.setOtpAttempts(MAX_LOGIN_ATTEMPTS);
                candidatesRepository.save(candidate);
                log.info("Candidate login successful");
                mailService.sendMobileOTP(candidate.getMobileNumber(), candidate.getEmail(), candidate.getFullName(), otp);

                SmsRequest smsRequest = SmsRequest.builder()
                        .phNumber(candidate.getMobileNumber())
                        .templateName(SmsTemplateType.OTP)
                        .otp(otp)
                        .build();
                smsService.sendSms(smsRequest);

                CandidateLoginPreOtpResponse response = CandidateLoginPreOtpResponse.builder()
                        .isLoginSuccess(true)
                        .canEditMobile(false)
                        .mobileNumber(commonUtilityProvider.maskMobileNumber(candidate.getMobileNumber()))
                        .otpExpiry(otpExpiry)
                        .build();
                return response;
            }else{
                // ----------------- PASSWORD IS INCORRECT -----------------
                int remainingAttempts;

                try {
                    remainingAttempts = Integer.parseInt(
                            Optional.ofNullable(candidate.getCandidateReasonLocked()).orElse(String.valueOf(MAX_LOGIN_ATTEMPTS))
                    );
                } catch (NumberFormatException ex) {
                    log.info("Entered Invalid Password");
                    remainingAttempts = MAX_LOGIN_ATTEMPTS;
                }
                remainingAttempts--;
                candidate.setCandidateReasonLocked(String.valueOf(remainingAttempts));

                if (remainingAttempts <= 0) {
                    candidate.setCandidateTempLock(true);
                    candidate.setCandidateReasonLocked(ACCOUNT_LOCKED_REASON);
                    candidate.setCandidateTempLockedDate(LocalDate.now());
                    candidatesRepository.save(candidate);
                    throw new AuthenticationException("Account locked due to too many failed attempts.");
                }
                candidatesRepository.save(candidate);
                log.info("Saved Remaining attempts to DB");
                throw new AuthenticationException("Invalid username or password. " + remainingAttempts + " attempts remaining.");
            }
        }else{
            // ----------------- UnVerified candidate -----------------


            Optional<CandidateRegistrationStagesEntity> optionalCandidateRegistrationStages =
                    candidateRegistrationStagesRepository.findByIsEmailVerifiedAndEmailIgnoreCase(true,credentials.getEmail());

            if(optionalCandidateRegistrationStages.isEmpty()){
                //throw new AuthenticationException("Email not yet registered/verified");
                throw new AuthenticationException("Invalid Username or password!");

            }

            CandidateRegistrationStagesEntity candidateRegistrationStages = optionalCandidateRegistrationStages.get();

            //Validate password
            if(!candidateRegistrationStages.getPasswordHash().equals(credentials.getPassword())){
                throw new AuthenticationException("Invalid Username or password!");
            }

            if(checkExistingPhoneNo(candidateRegistrationStages.getMobileNumber())){
                CandidateLoginPreOtpResponse response =
                        CandidateLoginPreOtpResponse.builder()
                                .isLoginSuccess(false)
                                .canEditMobile(true)
                                .responseMessage("Mobile number already registered to a different User!")
                                .build();
                return response;
            }

            String otp =  generateRandomOtp();
            LocalDateTime otpExpiry =LocalDateTime.now().plusMinutes(Integer.parseInt(otpExpirationMinutes));
            candidateRegistrationStages.setOtpCode(otp);
            candidateRegistrationStages.setOtpExpiry(otpExpiry);
            candidateRegistrationStages.setOtpAttempts(MAX_LOGIN_ATTEMPTS);

            candidateRegistrationStagesRepository.save(candidateRegistrationStages);
            log.info("Generated OTP saved to DB");
            mailService.sendMobileOTP(candidateRegistrationStages.getMobileNumber(), candidateRegistrationStages.getEmail(), candidateRegistrationStages.getFullName(), otp);

            SmsRequest smsRequest = SmsRequest.builder()
                    .phNumber(candidateRegistrationStages.getMobileNumber())
                    .templateName(SmsTemplateType.OTP)
                    .otp(otp)
                    .build();
            smsService.sendSms(smsRequest);

            CandidateLoginPreOtpResponse response =
                    CandidateLoginPreOtpResponse.builder()
                            .isLoginSuccess(true)
                            .mobileNumber(commonUtilityProvider.maskMobileNumber(candidateRegistrationStages.getMobileNumber()))
                            .canEditMobile(true)
                            .otpExpiry(otpExpiry)
                            .responseMessage("OTP sent to mobile and email!")
                            .build();
            return response;
        }
    }

    @Transactional(dontRollbackOn = AuthenticationException.class)
    public CandidateLoginResponse verifyOtp(String email, String inputOtp, HttpServletRequest request, HttpServletResponse response) {

        // Validate session limit BEFORE processing OTP
        candidateSessionManagementService.validateSessionLimit(email);

        Optional<CandidatesEntity> optionalCandidate = candidatesRepository.findByEmailIgnoreCase(email);

        // Candidate first time login
        if(optionalCandidate.isEmpty()) {

            CandidateRegistrationStagesEntity candidateRegistrationStages = candidateRegistrationStagesRepository.findByIsEmailVerifiedAndEmailIgnoreCase(true,email)
                    .orElseThrow(() -> new ResourceNotFoundException(CANDIDATE_NOT_FOUND_MESSAGE));

            if(checkExistingPhoneNo(candidateRegistrationStages.getMobileNumber())){
                throw new AuthenticationException("Mobile number already registered to a different User!");
            }

            //To Check if OTP attempts exceeded
            if(candidateRegistrationStages.getOtpAttempts() != null && candidateRegistrationStages.getOtpAttempts() <= 0 && candidateRegistrationStages.getOtpExpiry() == null) {
                throw new AuthenticationException("Too many failed OTP attempts.Please request a new OTP.");
            }

            if (candidateRegistrationStages.getOtpExpiry() == null || candidateRegistrationStages.getOtpExpiry().isBefore(LocalDateTime.now())) {
                throw new AuthenticationException("Request timed out. Please request a new OTP.");
            }

            // 3: Check OTP match
            if (!candidateRegistrationStages.getOtpCode().equals(inputOtp)) {
                int remainingAttempts = candidateRegistrationStages.getOtpAttempts()!=null ? candidateRegistrationStages.getOtpAttempts()-1
                        : MAX_LOGIN_ATTEMPTS-1;
                candidateRegistrationStages.setOtpAttempts(remainingAttempts);
                if(remainingAttempts <= 0) {
                    candidateRegistrationStages.setOtpCode(null);
                    candidateRegistrationStages.setOtpExpiry(null);
                    candidateRegistrationStagesRepository.save(candidateRegistrationStages);
                    throw new AuthenticationException("Too many failed OTP attempts.Please request a new OTP.");
                }
                candidateRegistrationStagesRepository.save(candidateRegistrationStages);
                throw new AuthenticationException("Invalid OTP");
            }

            // 4: OTP is valid — clear OTP so it can't be reused
            candidateRegistrationStages.setOtpCode(null);
            candidateRegistrationStages.setOtpExpiry(null);
            candidateRegistrationStages.setOtpAttempts(MAX_LOGIN_ATTEMPTS);
            candidateRegistrationStages.setIsOtpVerified(true);

            String refreshToken = UUID.randomUUID().toString();

            CandidatesEntity candidatesEntity = CandidatesEntity.builder()
                    .fullName(candidateRegistrationStages.getFullName())
                    .email(candidateRegistrationStages.getEmail())
                    .mobileNumber(candidateRegistrationStages.getMobileNumber())
                    .passwordHash(candidateRegistrationStages.getPasswordHash())
                    .refreshToken(refreshToken)
                    .dateOfBirth(candidateRegistrationStages.getDateOfBirth())
                    .finalDeclarationAccepted(false)
                    .passwordLastChangedAt(LocalDateTime.now())
                    .passwordExpiryDate(LocalDateTime.now().plusDays(90))
                    .isEmailVerified(candidateRegistrationStages.getIsEmailVerified())
                    .otpAttempts(MAX_LOGIN_ATTEMPTS)
                    .privacyNoticeAccepted(candidateRegistrationStages.getPrivacyNoticeAccepted())
                    .build();

            CandidatesEntity saved = candidatesRepository.save(candidatesEntity);
            log.info("Candidate OTP verification successful");

            //successfull registration mail sent only during first time login
            mailService.sendSuccessFullRegistrationMail(saved);

            //mobile message for registration success
            SmsRequest smsRequest = SmsRequest.builder()
                .phNumber(saved.getMobileNumber())
                .templateName(SmsTemplateType.REGISTRATION)
                .regNo(saved.getRegistrationNo())
                .build();
            smsService.sendSms(smsRequest);
            log.info("Registration SMS sent successfully to candidate {}", saved.getId());

            String accessToken = jwtUtil.generateAccessToken(saved.getId(),email);
            log.info("Access token generated");

            response.addCookie(jwtUtil.createCookie(DBConstants.COOKIE_ACCESS_TOKEN, accessToken, 5 * 60)); // 5 min
            response.addCookie(jwtUtil.createCookie(DBConstants.COOKIE_REFRESH_TOKEN, refreshToken, 30 * 24 * 60 * 60)); // 30 days

            log.info("Cookies generated with Access and Refresh tokens");

            // Create session tracking record
            candidateSessionManagementService.createSession(email, refreshToken, request);

            candidateRegistrationStagesRepository.delete(candidateRegistrationStages);

            log.info("Removing candidate from  candidate registration stages as OTP verified");

            return CandidateLoginResponse.builder()
                    .user(
                            GetCandidateData.builder()
                                    .id(saved.getId())
                                    .name(saved.getFullName())
                                    .email(saved.getEmail())
                                    .currentStep(saved.getCurrentStep())
                                    .isProfileCompleted(saved.getIsProfileCompleted())
                                    .build()
                    )
                    .accessToken(accessToken)
                    .build();
        }else{

            // check OTP from candidate table and pass through
            CandidatesEntity candidate = optionalCandidate.get();

            //1.To Check if OTP attempts exceeded
            if(candidate.getOtpAttempts() != null && candidate.getOtpAttempts() <= 0 && candidate.getOtpExpiry() == null) {
                throw new AuthenticationException("Too many failed OTP attempt. Please request a new OTP.");
            }

            // 2: Check if OTP expired
            if (candidate.getOtpExpiry() == null || candidate.getOtpExpiry().isBefore(LocalDateTime.now())) {
                throw new AuthenticationException("Request timed out. Please request a new OTP.");
            }

            // 3: Check OTP match
            if (!candidate.getOtpCode().equals(inputOtp)) {
                int remainingAttempts = candidate.getOtpAttempts() != null ? candidate.getOtpAttempts() - 1
                        :MAX_LOGIN_ATTEMPTS-1;
                candidate.setOtpAttempts(remainingAttempts);
                if(remainingAttempts <= 0) {
                    candidate.setOtpCode(null);
                    candidate.setOtpExpiry(null);
                    candidatesRepository.save(candidate);
                    throw new AuthenticationException("Too many failed OTP attempts.Current OTP invalidated. Please request a new OTP.");
                }
                candidatesRepository.save(candidate);
                throw new AuthenticationException("Invalid OTP");
            }

            // 4: OTP is valid — clear OTP so it can't be reused
            candidate.setOtpCode(null);
            candidate.setOtpExpiry(null);
            candidate.setOtpAttempts(MAX_LOGIN_ATTEMPTS);

            String refreshToken = UUID.randomUUID().toString();
            candidate.setRefreshToken(refreshToken);
            CandidatesEntity savedCandidate =candidatesRepository.save(candidate);
            log.info("Candidate Login Success -> (not first time)");
            String accessToken = jwtUtil.generateAccessToken(candidate.getId(),email);
            log.info("Access token generated");
            response.addCookie(jwtUtil.createCookie(DBConstants.COOKIE_ACCESS_TOKEN, accessToken, 5 * 60)); // 5 min
            response.addCookie(jwtUtil.createCookie(DBConstants.COOKIE_REFRESH_TOKEN, refreshToken, 30 * 24 * 60 * 60)); // 30 days
            log.info("Cookies generated with Access and Refresh tokens");

            // Create session tracking record
            candidateSessionManagementService.createSession(email, refreshToken, request);

            return CandidateLoginResponse.builder().user(
                            GetCandidateData.builder()
                                    .id(candidate.getId())
                                    .name(candidate.getFullName())
                                    .currentStep(savedCandidate.getCurrentStep())
                                    .isProfileCompleted(savedCandidate.getIsProfileCompleted())
                                    .email(candidate.getEmail())
                                    .build()
                    )
                    .accessToken(accessToken)
                    .build();
        }
    }

    public String logout(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null) {
            candidatesRepository.findByRefreshToken(refreshToken).ifPresent(user -> {
                user.setRefreshToken(null);
                candidatesRepository.save(user);
            });
            log.info("Candidate logged out successfully, refresh token invalidated");

            // Delete session tracking record using the refresh token UUID directly
            candidateSessionManagementService.deleteSessionByRefreshToken(refreshToken);
        }else {
            throw new AuthenticationException("Invalid or missing refresh token");
        }
        response.addCookie(jwtUtil.createCookie(DBConstants.COOKIE_ACCESS_TOKEN, "", 0));
        response.addCookie(jwtUtil.createCookie(DBConstants.COOKIE_REFRESH_TOKEN, "", 0));
        log.info("Cleared Access and Refresh token cookies");

        return "Logged out successfully!";
    }


    private String generateRandomOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    @Transactional
    public String refreshToken(String refreshToken, HttpServletResponse httpResponse) {
        CandidatesEntity candidate = candidatesRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid refresh token!"));

        // Email verification check
        if (!Boolean.TRUE.equals(candidate.getIsEmailVerified())) {
            throw new AuthenticationException("Email not verified. Please verify before continuing.");
        }

        // Account Lock Check
        if (candidate.getCandidateTempLock()) {
            throw new AuthenticationException("Account locked. Reason: " + candidate.getCandidateReasonLocked());
        }

        try {
            // Generate new access and refresh token
            String newAccessToken = jwtUtil.generateAccessToken(candidate.getId(),candidate.getEmail());
            String newRefreshToken = UUID.randomUUID().toString();

            // Update refresh token
            candidate.setRefreshToken(newRefreshToken);
            candidatesRepository.save(candidate);
            log.info("Update refresh token in DB");

            // Set cookies
            httpResponse.addCookie(jwtUtil.createCookie(DBConstants.COOKIE_ACCESS_TOKEN, newAccessToken, 5 * 60)); // 5 min
            httpResponse.addCookie(jwtUtil.createCookie(DBConstants.COOKIE_REFRESH_TOKEN, newRefreshToken, 30 * 24 * 60 * 60)); // 30 days
            log.info("Added updated access and refresh tokens to response headers");

            return "Token refreshed successfully";

        } catch (Exception ex) {
            throw new AuthenticationException("Failed to refresh token. Please login again.");
        }
    }


    @Transactional
    public String forgotPassword(String email) throws Exception{
        CandidatesEntity candidate = candidatesRepository.findByEmailIgnoreCase(email)
                .orElseThrow(()->new ResourceNotFoundException("Email not found"));

        String otp = generateRandomOtp();
        LocalDateTime otpExpiry =LocalDateTime.now().plusMinutes(Integer.parseInt(otpExpirationMinutes));

        mailService.sendMobileOTP(candidate.getMobileNumber(), candidate.getEmail(), candidate.getFullName(), otp);

        SmsRequest smsRequest = SmsRequest.builder()
                .phNumber(candidate.getMobileNumber())
                .templateName(SmsTemplateType.OTP)
                .otp(otp)
                .build();
        smsService.sendSms(smsRequest);

        candidate.setOtpCode(otp);
        candidate.setOtpExpiry(otpExpiry);
        candidate.setOtpAttempts(MAX_LOGIN_ATTEMPTS);
        candidatesRepository.save(candidate);
        log.info("Generated Forgot-Password OTP saved to DB");
        return "Password reset email sent.";
    }

    @Transactional(dontRollbackOn = AuthenticationException.class)
    public String passwordOtpVerify(String email, String otp) {
        CandidatesEntity candidate = candidatesRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(CANDIDATE_NOT_FOUND_MESSAGE));

        if(candidate.getOtpAttempts() != null && candidate.getOtpAttempts() <= 0 && candidate.getOtpExpiry() == null) {
            throw new AuthenticationException("Too many failed OTP attempts. Please request a new OTP.");
        }

        if (candidate.getOtpExpiry() == null || candidate.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("Request timed out. Please request a new OTP.");
        }

        if (!candidate.getOtpCode().equals(otp)) {
            int remainingAttempts = candidate.getOtpAttempts() != null ? candidate.getOtpAttempts() - 1
                    :MAX_LOGIN_ATTEMPTS-1;
            candidate.setOtpAttempts(remainingAttempts);
            if(remainingAttempts <= 0) {
                candidate.setOtpCode(null);
                candidate.setOtpExpiry(null);
                candidatesRepository.save(candidate);
                throw new AuthenticationException("Too many failed OTP attempts.Please request a new OTP.");
            }
            candidatesRepository.save(candidate);
            throw new AuthenticationException("Invalid OTP");
        }
        candidate.setOtpAttempts(MAX_LOGIN_ATTEMPTS);
        candidatesRepository.save(candidate);
        log.info("OTP verified successfully");
        return "OTP verified successfully.";
    }

    @Transactional(dontRollbackOn = AuthenticationException.class)
    public String resetPassword(ResetPasswordRequest request) {
        CredModel credentials = rsaDecryptionUtil.decryptToken(request.getCredentials());
        CandidatesEntity candidate = candidatesRepository.findByEmailIgnoreCase(credentials.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(CANDIDATE_NOT_FOUND_MESSAGE));

        if(candidate.getOtpAttempts() != null && candidate.getOtpAttempts() <= 0 && candidate.getOtpExpiry() == null) {
            throw new AuthenticationException("Something went wrong. Please request a new OTP.");
        }

        if (candidate.getOtpExpiry() == null || candidate.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("Request timed out. Please request a new OTP.");
        }

        if (!candidate.getOtpCode().equals(request.getOtp())) {
            int remainingAttempts = candidate.getOtpAttempts() != null ? candidate.getOtpAttempts() - 1
                    :MAX_LOGIN_ATTEMPTS-1;
            candidate.setOtpAttempts(remainingAttempts);
            if(remainingAttempts <= 0) {
                candidate.setOtpCode(null);
                candidate.setOtpExpiry(null);
            }
            candidatesRepository.save(candidate);
            throw new AuthenticationException("Something went wrong. Please request a new OTP.");
        }

        candidate.setOtpCode(null);
        candidate.setOtpExpiry(null);
        candidate.setOtpAttempts(MAX_LOGIN_ATTEMPTS);
        candidate.setPasswordHash(credentials.getPassword());
        candidate.setPasswordLastChangedAt(LocalDateTime.now());
        candidate.setPasswordExpiryDate(LocalDateTime.now().plusDays(90));
        candidatesRepository.save(candidate);
        log.info("Password reset successful");
        return "Password has been reset successfully.";
    }

    public Boolean checkExistingEmail(String emailId) {
        return (candidateRegistrationStagesRepository.existsByEmailIgnoreCaseAndIsEmailVerified(emailId,true)
        || candidatesRepository.existsByEmailIgnoreCase(emailId));
    }

    public Boolean checkExistingPhoneNo(String phoneNo) {
        return (candidateRegistrationStagesRepository.existsByMobileNumberAndIsOtpVerified(phoneNo,true)
        || candidatesRepository.existsByMobileNumber(phoneNo));
    }

    public CandidateLoginPreOtpResponse editMobileNo(@Valid CandidateLoginRequest request, String newMobileNo) throws Exception{

        CredModel credentials = rsaDecryptionUtil.decryptToken(request.getCredentials());

        if (credentials == null ) {
            throw new AuthenticationException("Invalid credentials format.");
        }

        Optional<CandidateRegistrationStagesEntity> optionalCandidateRegistrationStages =
                candidateRegistrationStagesRepository.findByIsEmailVerifiedAndEmailIgnoreCase(true,credentials.getEmail());

        if(optionalCandidateRegistrationStages.isEmpty()){
            //throw new AuthenticationException("Email not yet registered/verified");
            throw new AuthenticationException("Invalid Username or password!");
        }

        CandidateRegistrationStagesEntity candidateRegistrationStages = optionalCandidateRegistrationStages.get();

        //Validate password
        if(!candidateRegistrationStages.getPasswordHash().equals(credentials.getPassword())){
            throw new AuthenticationException("Invalid Username or password!");
        }

        candidateRegistrationStages.setMobileNumber(newMobileNo);

        if(checkExistingPhoneNo(candidateRegistrationStages.getMobileNumber())){
            CandidateLoginPreOtpResponse response =
                    CandidateLoginPreOtpResponse.builder()
                            .isLoginSuccess(false)
                            .canEditMobile(true)
                            .responseMessage("Mobile number already registered to a different User!")
                            .build();
            return response;
        }

        String otp =  generateRandomOtp();
        LocalDateTime otpExpiry =LocalDateTime.now().plusMinutes(Integer.parseInt(otpExpirationMinutes));
        candidateRegistrationStages.setOtpCode(otp);
        candidateRegistrationStages.setOtpExpiry(otpExpiry);
        candidateRegistrationStages.setOtpAttempts(MAX_LOGIN_ATTEMPTS);

        candidateRegistrationStagesRepository.save(candidateRegistrationStages);
        log.info("Mobile number changed and Generated OTP saved to DB");
        mailService.sendMobileOTP(candidateRegistrationStages.getMobileNumber(), candidateRegistrationStages.getEmail(), candidateRegistrationStages.getFullName(), otp);

        SmsRequest smsRequest = SmsRequest.builder()
                .phNumber(candidateRegistrationStages.getMobileNumber())
                .templateName(SmsTemplateType.OTP)
                .otp(otp)
                .build();
        smsService.sendSms(smsRequest);

        CandidateLoginPreOtpResponse response =
                CandidateLoginPreOtpResponse.builder()
                        .isLoginSuccess(true)
                        .canEditMobile(true)
                        .mobileNumber(commonUtilityProvider.maskMobileNumber(candidateRegistrationStages.getMobileNumber()))
                        .responseMessage("OTP sent to mobile and email!")
                        .build();
        return response;
    }


    public LocalDateTime resendOtp(String email) {
        Optional<CandidatesEntity> optionalCandidatesEntity=candidatesRepository.findByEmailIgnoreCase(email);
        LocalDateTime otpExpiry=null;
        if(optionalCandidatesEntity.isPresent()){
            //Get candidate info
            CandidatesEntity candidate = optionalCandidatesEntity.get();
            log.info("Candidate:{}",candidate);
            //Gen random otp
            String otp = generateRandomOtp();
            //Check whether the account is locked or not.
            if(candidate.getCandidateTempLock() != null && candidate.getCandidateTempLock()){
                throw new AuthenticationException("Cannot resend OTP. Account is locked. Reason: " + candidate.getCandidateReasonLocked());
            }
            otpExpiry =LocalDateTime.now().plusMinutes(Integer.parseInt(otpExpirationMinutes));
            candidate.setOtpCode(otp);
            candidate.setOtpExpiry(otpExpiry);
            candidate.setOtpAttempts(MAX_LOGIN_ATTEMPTS);
            candidatesRepository.save(candidate);
            log.info("Resent OTP saved to DB");

            mailService.sendMobileOTP(candidate.getMobileNumber(), candidate.getEmail(), candidate.getFullName(), otp);

            //Generate the sms request and send sms
            SmsRequest smsRequest = SmsRequest.builder()
                    .phNumber(candidate.getMobileNumber())
                    .templateName(SmsTemplateType.OTP)
                    .otp(otp)
                    .build();
            smsService.sendSms(smsRequest);
        }
        else{
            //check in reg table
            CandidateRegistrationStagesEntity candidateRegistrationStages = candidateRegistrationStagesRepository.findTopByEmailIgnoreCaseOrderByCreatedDateDesc(email)
                    .orElseThrow(() -> new ResourceNotFoundException(CANDIDATE_NOT_FOUND_MESSAGE));
            //gen otp and save
            String otp =  generateRandomOtp();
            otpExpiry =LocalDateTime.now().plusMinutes(Integer.parseInt(otpExpirationMinutes));
            candidateRegistrationStages.setOtpCode(otp);
            candidateRegistrationStages.setOtpExpiry(otpExpiry);
            candidateRegistrationStages.setOtpAttempts(MAX_LOGIN_ATTEMPTS);

            candidateRegistrationStagesRepository.save(candidateRegistrationStages);
            log.info("Resent OTP saved to DB");

            mailService.sendMobileOTP(candidateRegistrationStages.getMobileNumber(), candidateRegistrationStages.getEmail(), candidateRegistrationStages.getFullName(), otp);

            SmsRequest smsRequest = SmsRequest.builder()
                    .phNumber(candidateRegistrationStages.getMobileNumber())
                    .templateName(SmsTemplateType.OTP)
                    .otp(otp)
                    .build();
            smsService.sendSms(smsRequest);
        }       
        return otpExpiry;
    }

}

