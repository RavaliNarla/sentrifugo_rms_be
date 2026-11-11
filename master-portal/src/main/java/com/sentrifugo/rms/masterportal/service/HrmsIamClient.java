package com.sentrifugo.rms.masterportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin IAM HTTP client for master-data sync. Uses the caller's HRMS Bearer token
 * (same pattern as auth-portal employee search).
 */
@Slf4j
@Component
public class HrmsIamClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String iamBaseUrl;

    public HrmsIamClient(
            ObjectMapper objectMapper,
            @Value("${hrms.iam.base-url:http://172.16.2.111:8000/api/iam}") String iamBaseUrl) {
        this.objectMapper = objectMapper;
        this.iamBaseUrl = iamBaseUrl;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public List<JsonNode> listDepartments(String hrmsBearerToken) {
        return listAll(hrmsBearerToken, "/departments/");
    }

    public List<JsonNode> listBusinessUnits(String hrmsBearerToken) {
        return listAll(hrmsBearerToken, "/business-units/?is_active=true");
    }

    public List<JsonNode> listDesignations(String hrmsBearerToken) {
        return listAll(hrmsBearerToken, "/designations/");
    }

    private List<JsonNode> listAll(String hrmsBearerToken, String pathWithOptionalQuery) {
        List<JsonNode> all = new ArrayList<>();
        int skip = 0;
        final int limit = 1000;
        String base = iamBase().endsWith("/") ? iamBase().substring(0, iamBase().length() - 1) : iamBase();
        String path = pathWithOptionalQuery.startsWith("/") ? pathWithOptionalQuery : "/" + pathWithOptionalQuery;
        boolean hasQuery = path.contains("?");
        while (true) {
            String sep = hasQuery ? "&" : "?";
            String uri = base + path + sep + "skip=" + skip + "&limit=" + limit;
            JsonNode body = getJson(hrmsBearerToken, uri);
            List<JsonNode> page = rows(body);
            all.addAll(page);
            if (page.size() < limit) {
                break;
            }
            skip += limit;
            if (skip > 20000) {
                log.warn("IAM list truncated at {} rows for {}", skip, path);
                break;
            }
        }
        return all;
    }

    private JsonNode getJson(String hrmsBearerToken, String uri) {
        try {
            String body = restClient.get()
                    .uri(uri)
                    .header("Authorization", bearer(hrmsBearerToken))
                    .header("accept", "application/json")
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body == null ? "[]" : body);
        } catch (RestClientResponseException ex) {
            log.warn("IAM GET {} → {}: {}", uri, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("HRMS master list failed (" + ex.getStatusCode().value() + ").");
        } catch (Exception ex) {
            log.warn("IAM GET {} failed: {}", uri, ex.getMessage());
            throw new IllegalStateException("HRMS master list is unavailable.");
        }
    }

    private static List<JsonNode> rows(JsonNode body) {
        if (body == null || body.isNull()) {
            return List.of();
        }
        if (body.isArray()) {
            List<JsonNode> list = new ArrayList<>();
            body.forEach(list::add);
            return list;
        }
        for (String key : new String[] {"items", "results", "data"}) {
            JsonNode node = body.get(key);
            if (node != null && node.isArray()) {
                List<JsonNode> list = new ArrayList<>();
                node.forEach(list::add);
                return list;
            }
        }
        return List.of();
    }

    private String iamBase() {
        return iamBaseUrl == null ? "" : iamBaseUrl.trim();
    }

    private static String bearer(String token) {
        if (token == null) {
            return "Bearer ";
        }
        String t = token.trim();
        return t.regionMatches(true, 0, "Bearer ", 0, 7) ? t : "Bearer " + t;
    }
}
