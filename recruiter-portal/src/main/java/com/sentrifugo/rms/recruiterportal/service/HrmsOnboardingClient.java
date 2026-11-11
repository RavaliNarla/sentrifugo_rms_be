package com.sentrifugo.rms.recruiterportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;

/**
 * HRMS IAM onboarding APIs ({@code POST /onboarding/resume} then {@code POST /onboarding}).
 * Auth is always the caller's HRMS Bearer token (same pattern as master sync / genders) —
 * never a configured/sample credential.
 */
@Slf4j
@Component
public class HrmsOnboardingClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String iamBaseUrl;

    public HrmsOnboardingClient(
            ObjectMapper objectMapper,
            @Value("${hrms.iam.base-url:http://172.16.2.111:8000/api/iam}") String iamBaseUrl) {
        this.objectMapper = objectMapper;
        this.iamBaseUrl = trimTrailingSlash(iamBaseUrl);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Uploads resume bytes; returns HRMS {@code assetId}. */
    public String uploadResume(String hrmsBearerToken, byte[] fileBytes, String fileName, String contentType) {
        requireToken(hrmsBearerToken);
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        String name = (fileName == null || fileName.isBlank()) ? "resume.pdf" : fileName;
        MediaType mediaType = contentType == null || contentType.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
        body.part("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return name;
            }
        }).contentType(mediaType);

        try {
            String response = restClient.post()
                    .uri(iamBaseUrl + "/onboarding/resume")
                    .header("Authorization", bearerHeader(hrmsBearerToken))
                    .header("accept", "*/*")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            String assetId = text(root, "assetId");
            if (assetId == null) {
                throw new IllegalStateException("HRMS resume upload returned no assetId: " + response);
            }
            return assetId;
        } catch (RestClientResponseException ex) {
            log.warn("HRMS resume upload → {}: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("HRMS resume upload failed (" + ex.getStatusCode().value() + ")");
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("HRMS resume upload failed: " + ex.getMessage(), ex);
        }
    }

    public String createOnboarding(String hrmsBearerToken, OnboardingPayload payload) {
        requireToken(hrmsBearerToken);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("firstName", nullToEmpty(payload.firstName()));
        body.put("middleName", blankAsSpace(payload.middleName()));
        body.put("lastName", nullToEmpty(payload.lastName()));
        if (payload.dateOfBirth() != null) {
            body.put("dateOfBirth", payload.dateOfBirth().toString());
        } else {
            body.putNull("dateOfBirth");
        }
        if (payload.genderHrmsId() != null && !payload.genderHrmsId().isBlank()) {
            body.put("gender", payload.genderHrmsId().trim());
        } else {
            body.putNull("gender");
        }
        body.put("personalEmail", nullToEmpty(payload.personalEmail()));
        body.put("personalPhone", nullToEmpty(payload.personalPhone()));
        body.put("isRMS", true);
        body.put("personalCountryCode", " ");
        body.put("requisitionId", payload.requisitionCode());
        body.putNull("reportingManagerId");
        putIdOrNull(body, "businessUnitId", payload.businessUnitHrmsId());
        putIdOrNull(body, "departmentId", payload.departmentHrmsId());
        putIdOrNull(body, "designationId", payload.designationHrmsId());
        if (payload.dateOfJoining() != null) {
            body.put("dateOfJoining", payload.dateOfJoining().toString());
        } else {
            body.putNull("dateOfJoining");
        }
        putIdOrNull(body, "resumeAssetId", payload.resumeAssetId());
        body.putNull("resumeExtract");

        try {
            String response = restClient.post()
                    .uri(iamBaseUrl + "/onboarding")
                    .header("Authorization", bearerHeader(hrmsBearerToken))
                    .header("accept", "*/*")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            String id = text(root, "id");
            if (id == null) {
                throw new IllegalStateException("HRMS onboarding create returned no id: " + response);
            }
            return id;
        } catch (RestClientResponseException ex) {
            log.warn("HRMS onboarding create → {}: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("HRMS onboarding create failed (" + ex.getStatusCode().value() + ")");
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("HRMS onboarding create failed: " + ex.getMessage(), ex);
        }
    }

    private static void requireToken(String hrmsBearerToken) {
        if (hrmsBearerToken == null || hrmsBearerToken.isBlank()) {
            throw new IllegalStateException("HRMS Authorization token is required for onboarding APIs");
        }
    }

    private static void putIdOrNull(ObjectNode body, String field, String value) {
        if (value == null || value.isBlank()) {
            body.putNull(field);
        } else {
            body.put(field, value.trim());
        }
    }

    private static String blankAsSpace(String value) {
        if (value == null || value.isBlank()) {
            return " ";
        }
        return value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String text(JsonNode n, String field) {
        if (n == null) {
            return null;
        }
        JsonNode v = n.get(field);
        if (v == null || v.isNull() || !v.isValueNode()) {
            return null;
        }
        String s = v.asText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String bearerHeader(String token) {
        String t = token.trim();
        return t.regionMatches(true, 0, "Bearer ", 0, 7) ? t : "Bearer " + t;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    public record OnboardingPayload(
            String firstName,
            String middleName,
            String lastName,
            LocalDate dateOfBirth,
            String genderHrmsId,
            String personalEmail,
            String personalPhone,
            String requisitionCode,
            String businessUnitHrmsId,
            String departmentHrmsId,
            String designationHrmsId,
            LocalDate dateOfJoining,
            String resumeAssetId
    ) {}
}
