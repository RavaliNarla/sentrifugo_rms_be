package com.sentrifugo.rms.recruiterportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

/**
 * HRMS IAM onboarding APIs ({@code POST /onboarding/resume} then {@code POST /onboarding}).
 * Auth is always the caller's HRMS Bearer token (same pattern as master sync / genders) —
 * never a configured/sample credential.
 */
@Slf4j
@Component
public class HrmsOnboardingClient {

    private static final String CRLF = "\r\n";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String iamBaseUrl;

    public HrmsOnboardingClient(
            ObjectMapper objectMapper,
            @Value("${hrms.iam.base-url:http://172.16.2.111:8000/api/iam}") String iamBaseUrl) {
        this.objectMapper = objectMapper;
        this.iamBaseUrl = trimTrailingSlash(iamBaseUrl);
        // HttpURLConnection (not JDK HttpClient) — uvicorn rejects JDK multipart framing.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Uploads resume bytes; returns HRMS {@code assetId}. */
    public String uploadResume(String hrmsBearerToken, byte[] fileBytes, String fileName, String contentType) {
        requireToken(hrmsBearerToken);
        String name = (fileName == null || fileName.isBlank()) ? "resume.pdf" : fileName;
        MediaType mediaType = contentType == null || contentType.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
        // Explicit boundary + body (same shape as curl -F / browser FormData). Spring's
        // MultipartBodyBuilder + JDK client still produces 400/422 against Mac uvicorn.
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] multipartBody;
        try {
            multipartBody = buildMultipartFileBody(boundary, "file", name, mediaType, fileBytes);
        } catch (IOException ex) {
            throw new IllegalStateException("HRMS resume upload failed: could not build multipart body", ex);
        }

        try {
            String response = restClient.post()
                    .uri(iamBaseUrl + "/onboarding/resume")
                    .header("Authorization", bearerHeader(hrmsBearerToken))
                    .header("accept", "*/*")
                    .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                    .body(multipartBody)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            String assetId = text(root, "assetId");
            if (assetId == null) {
                throw new IllegalStateException("HRMS resume upload returned no assetId: " + response);
            }
            return assetId;
        } catch (RestClientResponseException ex) {
            String bodyText = ex.getResponseBodyAsString();
            log.warn("HRMS resume upload → {}: {}", ex.getStatusCode().value(), bodyText);
            throw new IllegalStateException(hrmsHttpError("HRMS resume upload failed", ex.getStatusCode().value(), bodyText));
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("HRMS resume upload failed: " + ex.getMessage(), ex);
        }
    }

    public String createOnboarding(String hrmsBearerToken, OnboardingPayload payload) {
        requireToken(hrmsBearerToken);
        ObjectNode body = objectMapper.createObjectNode();
        // Names / country code: JSON null when blank — never a placeholder space.
        // HRMS CreateOnboardingRequest: middleName + personalCountryCode are Optional;
        // lastName is still required (min_length=1) by IAM schema when present on the candidate.
        putTextOrNull(body, "firstName", payload.firstName());
        putTextOrNull(body, "middleName", payload.middleName());
        putTextOrNull(body, "lastName", payload.lastName());
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
        putTextOrNull(body, "personalEmail", payload.personalEmail());
        putTextOrNull(body, "personalPhone", payload.personalPhone());
        body.put("isRMS", true);
        body.putNull("personalCountryCode");
        putTextOrNull(body, "requisitionId", payload.requisitionCode());
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
            String bodyText = ex.getResponseBodyAsString();
            log.warn("HRMS onboarding create → {}: {}", ex.getStatusCode().value(), bodyText);
            throw new IllegalStateException(hrmsHttpError("HRMS onboarding create failed", ex.getStatusCode().value(), bodyText));
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("HRMS onboarding create failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Builds a single-file multipart body matching browser / {@code curl -F} shape.
     * Header {@code Content-Type} must include the same {@code boundary}.
     */
    private static byte[] buildMultipartFileBody(
            String boundary,
            String fieldName,
            String fileName,
            MediaType partContentType,
            byte[] fileBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(fileBytes.length + 512);
        String safeName = fileName.replace("\"", "_");
        out.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + fieldName
                + "\"; filename=\"" + safeName + "\"" + CRLF).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + partContentType + CRLF).getBytes(StandardCharsets.UTF_8));
        out.write(CRLF.getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.write(CRLF.getBytes(StandardCharsets.UTF_8));
        out.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static String hrmsHttpError(String prefix, int status, String body) {
        if (body == null || body.isBlank()) {
            return prefix + " (" + status + ")";
        }
        String trimmed = body.trim();
        if (trimmed.length() > 400) {
            trimmed = trimmed.substring(0, 400) + "…";
        }
        return prefix + " (" + status + "): " + trimmed;
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

    private static void putTextOrNull(ObjectNode body, String field, String value) {
        if (value == null || value.isBlank()) {
            body.putNull(field);
        } else {
            body.put(field, value.trim());
        }
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
