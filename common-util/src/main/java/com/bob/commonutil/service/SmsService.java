package com.bob.commonutil.service;

import com.bob.commonutil.enums.SmsTemplateType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bob.commonutil.util.SmsEncryptionUtilApim;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.KeyGenerator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.bob.commonutil.model.SmsRequest;

@Slf4j
@Service
public class SmsService {

    @Value("${sms.gateway.uri}")
    private String gatewayUri;

    @Value("${sms.api.key}")
    private String apiKey;

    @Value("${sms.channel}")
    private String channel;

    @Value("${sms.templateId.jobapply}")
    private String templateIdJobApply;

    @Value("${sms.templateId.registration}")
    private String templateIdRegistration;

    @Value("${sms.templateId.otp}")
    private String templateIdOtp;

    @Value("${sms.oauth.client.id}")
    private String oauthClientId;

    @Value("${sms.oauth.client.secret}")
    private String oauthClientSecret;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SmsEncryptionUtilApim encryptionUtil;

    private final Map<String, OAuthToken> tokenCache = new ConcurrentHashMap<>();


    public SmsResponse sendSms(SmsRequest smsRequest){
        try {
            log.info("Starting SMS sending process for template type: {}", smsRequest.getTemplateName());
            log.debug("Processing SMS request for phone number ending with: {}", maskPhoneNumber(smsRequest.getPhNumber()));

            // Step 0: Get OAuth token
            String accessToken = getOAuthToken();
            log.debug("Successfully obtained OAuth token");

            // Step 1: Generate ephemeral symmetric keys
            KeyPair symmetricKeys = generateSymmetricKeys();
            String hexKey = symmetricKeys.getHexKey();
            String hexIv = symmetricKeys.getHexIv();
            String keyIvString = hexKey + "|" + hexIv;
            log.debug("Generated symmetric keys for encryption");

            // Step 2: Encrypt the keys with RSA using utility
            String eky = encryptionUtil.encryptSymKey(keyIvString);
            log.debug("Encrypted symmetric keys with RSA");

            // Step 3: Prepare and encrypt the payload
            Map<String, Object> payload = createPayload(smsRequest);
            String payloadJson = objectMapper.writeValueAsString(payload);
            log.debug("Created SMS payload with template ID: {}", payload.get("templateId"));

            byte[] keyBytes = SmsEncryptionUtilApim.hexToByte(hexKey);
            byte[] ivBytes = SmsEncryptionUtilApim.hexToByte(hexIv);
            String data = encryptionUtil.encryptPayload(payloadJson, keyBytes, ivBytes);
            log.debug("Encrypted SMS payload");

            // Step 4: Generate digital signature using utility
            String sign = encryptionUtil.genSign(payloadJson);
            log.debug("Generated digital signature for payload");

            // Step 5: Construct the final request
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("eky", eky);
            requestBody.put("data", data);
            requestBody.put("sign", sign);
            log.debug("Constructed final SMS request body");

            // Convert requestBody to JSON for the API request
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            // Send request
            HttpHeaders headers = createHeaders(accessToken);
            log.debug("Request headers: {}", headers);
            log.debug("Request payload: {}", requestBodyJson);
            HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);

            // Log curl equivalent for debugging
            StringBuilder curlCmd = new StringBuilder(String.format("curl -X POST '%s/GenericSmsSvc/v1/sendSms'", gatewayUri));

            // Add all headers dynamically
            headers.forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    curlCmd.append(String.format(" -H '%s: %s'", key, values.get(0)));
                }
            });

            // Add request body
            curlCmd.append(String.format(" -d '%s'", requestBodyJson));
            log.info("Curl equivalent: {}", curlCmd);

            log.info("Sending SMS request to gateway: {}", gatewayUri);

            try {
                ResponseEntity<SmsResponse> response = restTemplate.exchange(
                        gatewayUri + "/GenericSmsSvc/v1/sendSms",
                        HttpMethod.POST,
                        entity,
                        SmsResponse.class
                );

                log.info("SMS request completed with status: {}", response.getStatusCode());
                if (response.getBody() != null) {
                    log.info("SMS reference number: {}", response.getBody().getSmsReferenceNumber());
                }
                return response.getBody();
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("SMS request failed with status: {}", e.getStatusCode());
                log.error("Response body: {}", e.getResponseBodyAsString());
                log.error("Response headers: {}", e.getResponseHeaders());
                throw e;
            } catch (RestClientException e) {
                // Try to get the raw response to see what the server actually returned
                try {
                    ResponseEntity<String> rawResponse = restTemplate.exchange(
                            gatewayUri + "/GenericSmsSvc/v1/sendSms",
                            HttpMethod.POST,
                            entity,
                            String.class
                    );
                    log.error("Raw response body: {}", rawResponse.getBody());
                    log.error("Raw response headers: {}", rawResponse.getHeaders());
                } catch (Exception ex) {
                    log.error("Failed to get raw response: {}", ex.getMessage());
                }
                log.error("SMS request failed: {}", e.getMessage());
                throw e;
            }
        }catch(Exception e){
            log.error("SMS request failed: {}", e.getMessage());
        }
        return null;
    }

    private KeyPair generateSymmetricKeys() throws Exception {
        // Generate 32-byte AES Key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        byte[] aesKey = keyGen.generateKey().getEncoded();

        // Generate 12-byte IV
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        String hexKey = bytesToHex(aesKey);
        String hexIv = bytesToHex(iv);

        return new KeyPair(hexKey, hexIv);
    }

    
    private Map<String, Object> createPayload(SmsRequest smsRequest) {
        Map<String, Object> payload = new HashMap<>();
        
        // Template ID registered with the bank based on template type
        String templateId = getTemplateId(smsRequest.getTemplateName());
        payload.put("templateId", templateId);
        
        // International SMS flag (N=No, Y=Yes)
        payload.put("isIntl", "N");
        
        // OTP flag (Y=Yes if this is an OTP SMS)
        payload.put("isOtp", smsRequest.getTemplateName() == SmsTemplateType.OTP ? "Y" : "N");
        
        // Phone number in international format (e.g., 919876543210)
        String phoneNumber = smsRequest.getPhNumber();
        if (phoneNumber != null && phoneNumber.length() == 10) {
            phoneNumber = "91" + phoneNumber;
        }
        payload.put("phoneNumber", phoneNumber);
        
        // Template variables - for OTP, this contains the OTP value
        switch (smsRequest.getTemplateName()) {
            case OTP:
                //Dear Candidate: {#numeric#} is the OTP for secure login to the Bank of Baroda Recruitment Portal. It is valid for 5 minutes. Do not share it with anyone.-BOB
                payload.put("templateVariables", new String[]{smsRequest.getOtp()});
                break;
            case JOBAPPLY:
                //Dear Candidate: Your application for the post of {#alphanumeric#} has been successfully submitted. Your Application ID is {#alphanumeric#}. Please retain it for future reference. - BOB
                payload.put("templateVariables", new String[]{smsRequest.getPostingName(), smsRequest.getApplicationId()});
                break;
            case REGISTRATION:
                //Dear Candidate: Your registration on the Bank of Baroda Recruitment Portal is successful. Your Registration No. is {#numeric#}. Please log in to proceed further. - BOB
                payload.put("templateVariables", new String[]{smsRequest.getRegNo()});
                break;
            default:
                throw new IllegalArgumentException("Unknown template type: " + smsRequest.getTemplateName());

        }

        return payload;
    }
    
    private String getTemplateId(SmsTemplateType smsTemplateType) {
        switch (smsTemplateType) {
            case JOBAPPLY:
                return templateIdJobApply;
            case REGISTRATION:
                return templateIdRegistration;
            case OTP:
                return templateIdOtp;
            default:
                throw new IllegalArgumentException("Unknown template type: " + smsTemplateType);
        }
    }

    private String getOAuthToken() throws Exception {
        String cacheKey = oauthClientId;
        OAuthToken cachedToken = tokenCache.get(cacheKey);
        
        // Check if token is still valid (with 5-minute buffer)
        if (cachedToken != null && cachedToken.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            log.debug("Using cached OAuth token, expires at: {}", cachedToken.getExpiresAt());
            return cachedToken.getAccessToken();
        }

        log.info("Requesting new OAuth token from authentication server");
        // Get new OAuth token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", "oob");
        body.add("client_id", oauthClientId);
        body.add("client_secret", oauthClientSecret);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        // Calculate and set Content-Length manually
        String formBody = "grant_type=client_credentials&scope=oob&client_id=" + oauthClientId + "&client_secret=" + oauthClientSecret;
        headers.setContentLength(formBody.getBytes(StandardCharsets.UTF_8).length);

        try {
            log.debug("Sending OAuth token request to authentication server");
            ResponseEntity<OAuthResponse> response = restTemplate.exchange(
                "https://apisbx.bankofbaroda.co.in/auth/oauth/v3/token",
                HttpMethod.POST,
                entity,
                OAuthResponse.class
            );

            log.debug("OAuth token response received with status: {}", response.getStatusCode());
            OAuthResponse oauthResponse = response.getBody();
            if (oauthResponse == null || oauthResponse.access_token == null) {
                log.error("Failed to obtain OAuth token - response was null or missing access token");
                throw new RuntimeException("Failed to obtain OAuth token - response was null or missing access token");
            }

            // Cache the token
            OAuthToken newToken = new OAuthToken(
                oauthResponse.access_token,
                LocalDateTime.now().plusSeconds(oauthResponse.expires_in)
            );
            tokenCache.put(cacheKey, newToken);
            log.info("Successfully obtained and cached OAuth token, expires in: {} seconds", oauthResponse.expires_in);

            return newToken.getAccessToken();
            
        } catch (Exception e) {
            log.error("Error getting OAuth token: {}", e.getMessage());
            throw new RuntimeException("Failed to obtain OAuth token: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Content-Type", "application/json");

        // OAuth bearer token for authentication
        headers.set("Authorization", "Bearer " + accessToken);
        
        // API key provided by the bank
        headers.set("apikey", apiKey);
        
        // Unique request identifier for tracking
        headers.set("requestId", UUID.randomUUID().toString());
        
        // Current timestamp in milliseconds
        headers.set("timestamp", java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        
        // Cache control to prevent caching
        headers.set("Cache-Control", "no-cache");
        
        // Channel through which the request is made
        headers.set("Channel", channel);
        
        return headers;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Data
    private static class KeyPair {
        private final String hexKey;
        private final String hexIv;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "******" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    @Data
    public static class SmsResponse {
        private String smsReferenceNumber;
        private String status;
        private String message;
    }

    @Data
    private static class OAuthToken {
        private final String accessToken;
        private final LocalDateTime expiresAt;
    }

    @Data
    private static class OAuthResponse {
        private String access_token;
        private String token_type;
        private Integer expires_in;
        private String scope;
        private String[] resource;
    }
}
