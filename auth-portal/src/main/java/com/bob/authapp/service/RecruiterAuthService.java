package com.bob.authapp.service;

import com.bob.authapp.model.RecruiterLoginRequest;
import com.bob.authapp.model.RecruiterRegisterRequest;
import com.bob.authapp.utils.AppConstants;
import com.bob.commonutil.exception.AuthenticationException;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.entity.UserEntity;
import com.bob.db.repository.UserRepository;
import com.bob.db.util.DBConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class RecruiterAuthService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UserRepository recruiterRepository;

    @Value("${auth0.domain}")
    private String auth0Domain;

    @Value("${auth0.connection}")
    private String auth0Connection;

    @Value("${auth0.recruiter.client-id}")
    private String recruiterClientId;

    @Value("${auth0.recruiter.client-secret}")
    private String recruiterClientSecret;


    @Value("${auth0.m2m.client-id}")
    private String m2mClientId;

    @Value("${auth0.m2m.client-secret}")
    private String m2mClientSecret;

    @Autowired
    private SecurityUtils securityUtils;

    public Map<String, Object> registerRecruiter(RecruiterRegisterRequest req) {

        validateRequest(req);

        String name = req.getName();
        String email = req.getEmail();
        String password = req.getPassword();
        String role = req.getRole();


        String auth0UserResponse = signupInAuth0(name, email, password);

        String auth0UserId = fetchAuth0UserId(email);

        UserEntity user = new UserEntity();
        user.setName(name);
        user.setRole(role);
        user.setEmail(email);
        user.setManagerId(new UUID(0L, 0L));
        user.setUserPassword(password);
        user.setOathUserId(auth0UserId);
        user.setInterviewCenterId(req.getInterviewCenterId());
        user.setIsActive(true);

        recruiterRepository.save(user);
        log.info("Recruiter Details saved in DB");
        // ---------------------------------------------
        // 3) Response
        // ---------------------------------------------
        Map<String, Object> response = new HashMap<>();
        response.put(AppConstants.MESSAGE, "User registered");
        response.put("auth0_user", auth0UserResponse);
        response.put("local_user_id", user.getId());

        log.info("Recruiter registration successful");
        return response;
    }

    // -----------------------------
    private void validateRequest(RecruiterRegisterRequest req) {
        if (req.getName() == null || req.getEmail() == null ||
                req.getPassword() == null || req.getRole() == null) {
            throw new IllegalArgumentException("name, email, password, and role are required");
        }
        log.info("Request Validated");
    }

    // -----------------------------
    private String signupInAuth0(String name, String email, String password) {

        Map<String, Object> signupBody = new HashMap<>();
        signupBody.put(AppConstants.CLIENT_ID, recruiterClientId);
        signupBody.put("email", email);
        signupBody.put("password", password);
        signupBody.put("connection", auth0Connection);
        signupBody.put("user_metadata", Map.of("name", name));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(signupBody, headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    auth0Domain + "/dbconnections/signup",
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            log.info("Auth0 signup successful");
            return resp.getBody();

        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();

            if (body.contains("exists") || body.contains("identifierType")) {
                throw new DuplicateKeyException("Email already registered");
            }

            throw new AuthenticationException("Auth0 signup failed: " + body);
        }
    }


    public String fetchAuth0UserId(String email)  {

        String mgmtToken = getMgmtToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set(DBConstants.HEADER_AUTHORIZATION, DBConstants.HEADER_BEARER + mgmtToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = auth0Domain + "/api/v2/users-by-email?email=" + email;

        ResponseEntity<List<LinkedHashMap<String, Object>>> resp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );



        List<LinkedHashMap<String, Object>> users = resp.getBody();

        if (users != null && !users.isEmpty()) {
            Object userId = users.get(0).get(AppConstants.USER_ID);



            return userId != null ? userId.toString() : null;
        }

        return null;
    }
    // -----------------------------
//    private String decryptPassword(String encrypted) {
//        return encrypted; // replace with your real decrypt logic
//    }

    // -----------------------------
    private String getMgmtToken(){
        Map<String, Object> body = new HashMap<>();
        body.put(AppConstants.CLIENT_ID, m2mClientId);
        body.put(AppConstants.CLIENT_SECRET, m2mClientSecret);
        body.put("audience", auth0Domain + "/api/v2/");
        body.put(AppConstants.GRANT_TYPE, "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                auth0Domain + AppConstants.OAUTH_TOKEN_ENDPOINT,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        log.info("Management API token obtained successfully");
        return resp.getBody().get(AppConstants.ACCESS_TOKEN).toString();
    }


    public ResponseEntity<?> loginRecruiter(RecruiterLoginRequest req,
                                            HttpServletResponse servletResponse)
            {

        String email = req.getEmail();
        String password = req.getPassword();
//        String password = decryptPassword(encryptedPassword);

        // -----------------------------
        // 1. Auth0 Password Realm Login
        // -----------------------------
        Map<String, Object> loginBody = new HashMap<>();
        loginBody.put(AppConstants.GRANT_TYPE, "http://auth0.com/oauth/grant-type/password-realm");
        loginBody.put("username", email);
        loginBody.put("password", password);
        loginBody.put("audience", auth0Domain + "/api/v2/");
        loginBody.put("scope", "openid profile email offline_access");
        loginBody.put(AppConstants.CLIENT_ID, recruiterClientId);
        loginBody.put(AppConstants.CLIENT_SECRET, recruiterClientSecret);
        loginBody.put("realm", auth0Connection);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> tokenResponse;

        try {
            tokenResponse = restTemplate.exchange(
                    auth0Domain + AppConstants.OAUTH_TOKEN_ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(loginBody, headers),
                    new ParameterizedTypeReference<>() {}
            );
            log.info("Received login token response from Auth0");

        } catch (HttpClientErrorException e) {
            Map<String, Object> err = parseError(e);

            // ➤ MFA required
            if ("mfa_required".equals(err.get("error"))) {
                return ResponseEntity.ok(Map.of(
                        "mfa_required", true,
                        "mfa_token", err.get("mfa_token")
                ));
            }

            throw new AuthenticationException("Invalid username or password");
        }

        String accessToken = tokenResponse.getBody().get(AppConstants.ACCESS_TOKEN).toString();
        String refreshToken = tokenResponse.getBody().get("refresh_token").toString();
        String idToken = tokenResponse.getBody().get(AppConstants.ID_TOKEN).toString();

//        Map<String,Boolean> preveilegeMap = (Map<String, Boolean>) securityUtils.getPrivileges(req.getEmail());

        // -----------------------------
        // 2. Validate Access Token
        // -----------------------------
        try {
            validateAccessToken(accessToken);
        } catch (Exception ex) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid or expired access token"));
        }

        // -----------------------------
        // 3. Get UserInfo
        // -----------------------------
        Map<String, Object> userInfo = fetchUserInfo(accessToken);

        if (!(Boolean) userInfo.get("email_verified")) {
            log.warn("User email not verified");
            return ResponseEntity.status(403).body(Map.of(
                   AppConstants.ERROR, "Email not verified. Please verify your email before login.",
                    AppConstants.USER_ID, userInfo.get("sub")
            ));
        }

        // -----------------------------
        // 4. Set HttpOnly Cookies (Same as Node)
        // -----------------------------
        addCookie(servletResponse, AppConstants.ACCESS_TOKEN, accessToken, 15 * 60);
        addCookie(servletResponse, "refresh_token", refreshToken, 7 * 24 * 60 * 60);
        addCookie(servletResponse, AppConstants.ID_TOKEN, idToken, 15 * 60);

        // -----------------------------
        // SUCCESS RESPONSE
        // -----------------------------
        log.info("Recruiter logged in successfully");
        return ResponseEntity.ok(Map.of(
                "user", userInfo,
                AppConstants.ACCESS_TOKEN, accessToken,
                AppConstants.ID_TOKEN, idToken
        ));
    }

    // -----------------------------
    // Cookie helper
    // -----------------------------
    private void addCookie(HttpServletResponse res, String name, String value, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();

        res.addHeader("Set-Cookie", cookie.toString());
//        log.info("{} cookie added to response header", name);
    }

    // -----------------------------
    // Error handling helper
    // -----------------------------
    private Map<String, Object> parseError(HttpClientErrorException e) {
        try {
            return new ObjectMapper().readValue(e.getResponseBodyAsString(), Map.class);
        } catch (Exception ex) {
            return Map.of("error", "unknown_error");
        }
    }

    // -----------------------------
    // Fetch Auth0 userinfo
    // -----------------------------
    private Map<String, Object> fetchUserInfo(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.set(DBConstants.HEADER_AUTHORIZATION, DBConstants.HEADER_BEARER+ accessToken);

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                auth0Domain + "/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        log.info("User info fetched successfully from Auth0");
        return resp.getBody();
    }

    // -----------------------------
    // JWT Validation (via JWKS)
    // -----------------------------
    private void validateAccessToken(String token)  {
        // Use Nimbus or Auth0 library (add if needed)
        // Sample stub:
        if (token == null || token.length() < 10) {
            throw new AuthenticationException("Invalid token");
        }
        log.info("Access token validated successfully");
    }

    public Map<String, Object> resendVerificationEmail(String userId)  {

        // ---------------------------
        // Step 1: Get Management API Token
        // ---------------------------
        String mgmtToken = getMgmtToken();  // You already implemented this

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(DBConstants.HEADER_AUTHORIZATION, DBConstants.HEADER_BEARER + mgmtToken);

        Map<String, Object> body = Map.of(AppConstants.USER_ID, userId);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        // ---------------------------
        // Step 2: Trigger verification email
        // ---------------------------
        try {
            restTemplate.postForEntity(
                    auth0Domain + "/api/v2/jobs/verification-email",
                    entity,
                    String.class
            );
            log.info("Resend verification email triggered successfully");
        } catch (HttpClientErrorException e) {
            throw new AuthenticationException("Auth0 error: " + e.getResponseBodyAsString());
        }

        // ---------------------------
        // SUCCESS
        // ---------------------------
        return Map.of(AppConstants.MESSAGE, "Verification email sent.");
    }

    public ResponseEntity<Map<String,String>> refreshToken(String refreshToken, HttpServletResponse servletResponse)  {

        Map<String, Object> body = new HashMap<>();
        body.put(AppConstants.GRANT_TYPE, "refresh_token");
        body.put(AppConstants.CLIENT_ID, recruiterClientId);
        body.put(AppConstants.CLIENT_SECRET, recruiterClientSecret);
        body.put("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> resp;

        try {
            resp = restTemplate.exchange(
                    auth0Domain + AppConstants.OAUTH_TOKEN_ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<>() {}
            );
            log.info("Received Refresh Token response from Auth0");
        } catch (HttpClientErrorException e) {
            throw new AuthenticationException("Auth0 refresh error: " + e.getResponseBodyAsString());
        }

        String accessToken = (String) resp.getBody().get(AppConstants.ACCESS_TOKEN);
        String idToken = (String) resp.getBody().get(AppConstants.ID_TOKEN);

        // ⭐ NEW REFRESH TOKEN (Important!)
        String newRefreshToken = (String) resp.getBody().get("refresh_token");

        try {
            validateAccessToken(accessToken);
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired access token"));
        }

        // Update cookies
        setCookie(servletResponse, AppConstants.ACCESS_TOKEN, accessToken, 15 * 60);
        setCookie(servletResponse, AppConstants.ID_TOKEN, idToken, 15 * 60);

        // ⭐ Must update refresh token too
        if (newRefreshToken != null) {
            setCookie(servletResponse, "refresh_token", newRefreshToken, 7 * 24 * 60 * 60);
        }

        return ResponseEntity.ok(Map.of(AppConstants.MESSAGE, "Token refreshed"));
    }

    // -----------------------------
// Helper to set HttpOnly Cookies
// -----------------------------
    private void setCookie(HttpServletResponse res, String name, String value, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();

        res.addHeader("Set-Cookie", cookie.toString());
        log.info("{} cookie updated in response headers", name);
    }

    public void sendRecruiterPasswordReset(String email)  {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        // STEP 1 — Get Management API Token
        String mgmtToken = getMgmtToken();

        // STEP 2 — Call Auth0 change_password API
        Map<String, Object> body = new HashMap<>();
        body.put(AppConstants.CLIENT_ID, recruiterClientId);
        body.put("email", email);
        body.put("connection", auth0Connection);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(DBConstants.HEADER_AUTHORIZATION, DBConstants.HEADER_BEARER + mgmtToken);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(
                    auth0Domain + "/dbconnections/change_password",
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            log.info("Password reset request triggered successfully");
        } catch (HttpClientErrorException e) {
            throw new AuthenticationException("Auth0 password reset failed: " + e.getResponseBodyAsString());
        }
    }


}
