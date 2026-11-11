package com.sentrifugo.rms.recruiterportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.recruiterportal.dto.HrmsGenderOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin IAM client for HRMS master-data lists (e.g. GENDERS). No local RMS copy table.
 */
@Slf4j
@Component
public class HrmsMasterDataClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String iamBaseUrl;
    private final String organisationId;

    public HrmsMasterDataClient(
            ObjectMapper objectMapper,
            @Value("${hrms.iam.base-url:http://172.16.2.111:8000/api/iam}") String iamBaseUrl,
            @Value("${hrms.organisation-id:6ad091063219a441797d644d}") String organisationId) {
        this.objectMapper = objectMapper;
        this.iamBaseUrl = iamBaseUrl;
        this.organisationId = organisationId;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public List<HrmsGenderOption> listGenders(String hrmsBearerToken) {
        if (hrmsBearerToken == null || hrmsBearerToken.isBlank()) {
            return List.of();
        }
        String base = iamBaseUrl == null ? "" : iamBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String uri = UriComponentsBuilder.fromUriString(base + "/master-data/")
                .queryParam("category", "GENDERS")
                .queryParam("organisation_id", organisationId)
                .build(true)
                .toUriString();
        try {
            String body = restClient.get()
                    .uri(uri)
                    .header("Authorization", bearer(hrmsBearerToken))
                    .header("accept", "application/json")
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body == null ? "[]" : body);
            if (!root.isArray()) {
                return List.of();
            }
            List<HrmsGenderOption> out = new ArrayList<>();
            for (JsonNode n : root) {
                if (n == null || n.isNull()) {
                    continue;
                }
                boolean active = !n.has("is_active") || n.get("is_active").asBoolean(true);
                if (!active) {
                    continue;
                }
                String id = text(n, "id");
                String value = text(n, "value");
                if (id == null || value == null) {
                    continue;
                }
                out.add(HrmsGenderOption.builder()
                        .id(id)
                        .key(text(n, "key"))
                        .name(value)
                        .build());
            }
            return out;
        } catch (RestClientResponseException ex) {
            log.warn("IAM genders → {}: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CommonException("Failed to load genders from HRMS (" + ex.getStatusCode().value() + ").");
        } catch (CommonException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("IAM genders failed: {}", ex.getMessage());
            throw new CommonException("HRMS genders are unavailable.");
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull() || !v.isValueNode()) {
            return null;
        }
        String s = v.asText();
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String bearer(String token) {
        String t = token.trim();
        return t.regionMatches(true, 0, "Bearer ", 0, 7) ? t : "Bearer " + t;
    }
}
