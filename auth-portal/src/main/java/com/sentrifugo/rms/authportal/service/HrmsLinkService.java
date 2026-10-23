package com.sentrifugo.rms.authportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sentrifugo.rms.authportal.dto.HrmsAccessResponse;
import com.sentrifugo.rms.authportal.dto.LoginResponse;
import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.service.JwtTokenService;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * The HRMS access token is checked with IAM the same way the HRMS app checks it.
 * IAM accepts a valid token even when Valkey has no session row, so this link does not
 * read Valkey.
 *
 * Email comes from IAM {@code /auth/me}. Employee code comes from IAM {@code /employees/},
 * because the office {@code /directory} search returns nobody. Both values must match one RMS user.
 * The employee list payload includes private HR fields, so search results passed to RMS
 * keep only name, work email, employee code, and designation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrmsLinkService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    @Value("${hrms.iam.base-url}")
    private String iamBaseUrl;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(directoryRequestFactory())
            .build();

    private static JdkClientHttpRequestFactory directoryRequestFactory() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    public HrmsAccessResponse access(String authorization) {
        String token = bearer(authorization);
        Optional<UserEntity> user = matchRmsUser(identity(token));
        if (user.isEmpty() || !visibleRole(user.get().getRole())) {
            return notListed();
        }
        UserEntity found = user.get();
        return HrmsAccessResponse.builder()
                .listed(true)
                .role(found.getRole())
                .privileges(approverPrivilege(found))
                .hasUpcomingInterviews(false)
                .build();
    }

    public LoginResponse enter(String authorization) {
        String token = bearer(authorization);
        UserEntity user = matchRmsUser(identity(token))
                .filter(u -> visibleRole(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for Recruitment."));
        String rmsToken = jwtTokenService.issueAccessToken(user);
        return LoginResponse.builder()
                .accessToken(rmsToken)
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

    /** One employee-list search. Caller must already be an RMS admin (enforced on the controller). */
    public JsonNode searchEmployees(String hrmsToken, String search) {
        String query = search == null ? "" : search.trim();
        if (query.length() < 3) {
            throw new CommonException("Enter at least 3 characters.");
        }
        if (hrmsToken == null || hrmsToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "HRMS session is missing.");
        }
        return employeeCards(employees(hrmsToken.trim(), query));
    }

    private SessionIdentity identity(String accessToken) {
        JsonNode me = authMe(accessToken);
        String email = firstText(me, "email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "HRMS login has no email.");
        }
        String empCode = employeeCode(accessToken, email.trim());
        return new SessionIdentity(email.trim(), empCode);
    }

    private JsonNode authMe(String accessToken) {
        String base = iamBase();
        try {
            String body = restClient.get()
                    .uri(base + "/auth/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode data = root.get("data");
            if ((text(root, "email") == null) && data != null && data.isObject()) {
                return data;
            }
            return root;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "HRMS login is not valid.");
            }
            log.warn("IAM /auth/me failed: {}", ex.getStatusCode().value());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "HRMS identity service is unavailable.");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("IAM /auth/me failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "HRMS identity service is unavailable.");
        }
    }

    /** Null when this login has no employee row. An employee-list outage still fails the call. */
    private String employeeCode(String accessToken, String email) {
        for (JsonNode item : employeeRows(employees(accessToken, email))) {
            String itemEmail = firstText(item, "workEmail", "work_email", "email");
            if (itemEmail == null || !itemEmail.equalsIgnoreCase(email)) {
                continue;
            }
            String code = firstText(item, "empCode", "emp_code");
            if (code != null && !code.isBlank()) {
                return code.trim();
            }
        }
        return null;
    }

    private JsonNode employees(String accessToken, String search) {
        String base = iamBase();
        try {
            String body = restClient.get()
                    .uri(base + "/employees/?search={search}&skip=0&limit=20", search)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body == null ? "[]" : body);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "HRMS login is not valid.");
            }
            if (ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This HRMS login cannot read the employee list.");
            }
            log.warn("IAM employee list failed: {}", ex.getStatusCode().value());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "HRMS employee list is unavailable.");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("IAM employee list failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "HRMS employee list is unavailable.");
        }
    }

    private JsonNode employeeCards(JsonNode body) {
        ArrayNode items = objectMapper.createArrayNode();
        for (JsonNode row : employeeRows(body)) {
            String email = firstText(row, "workEmail", "work_email", "email");
            String code = firstText(row, "empCode", "emp_code");
            String name = displayName(row);
            if ((email == null || email.isBlank()) && (code == null || code.isBlank()) && (name == null || name.isBlank())) {
                continue;
            }
            ObjectNode card = objectMapper.createObjectNode();
            card.put("fullName", name == null ? "" : name);
            card.put("empCode", code == null ? "" : code);
            card.put("email", email == null ? "" : email);
            String designation = firstText(row, "designationName", "designation_name", "designation");
            card.put("designation", designation == null ? "" : designation);
            items.add(card);
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.set("items", items);
        result.put("total", items.size());
        return result;
    }

    private static Iterable<JsonNode> employeeRows(JsonNode body) {
        if (body == null || body.isNull()) {
            return java.util.List.of();
        }
        if (body.isArray()) {
            return body;
        }
        JsonNode items = body.path("items");
        if (!items.isArray()) {
            items = body.path("results");
        }
        if (!items.isArray() && body.path("data").isArray()) {
            items = body.path("data");
        }
        return items.isArray() ? items : java.util.List.of();
    }

    private static String displayName(JsonNode row) {
        String name = firstText(row, "fullName", "full_name");
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        String first = firstText(row, "firstName", "first_name");
        String middle = firstText(row, "middleName", "middle_name");
        String last = firstText(row, "lastName", "last_name");
        StringBuilder joined = new StringBuilder();
        for (String part : new String[] {first, middle, last}) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(part.trim());
        }
        return joined.length() == 0 ? null : joined.toString();
    }

    private String iamBase() {
        return iamBaseUrl.endsWith("/") ? iamBaseUrl.substring(0, iamBaseUrl.length() - 1) : iamBaseUrl;
    }

    private Optional<UserEntity> matchRmsUser(SessionIdentity identity) {
        if (identity.empCode() == null || identity.empCode().isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(identity.email())
                .filter(user -> user.getEmployeeId() != null
                        && user.getEmployeeId().equalsIgnoreCase(identity.empCode()));
    }

    private static HrmsAccessResponse notListed() {
        return HrmsAccessResponse.builder()
                .listed(false)
                .hasUpcomingInterviews(false)
                .build();
    }

    private String approverPrivilege(UserEntity user) {
        var privileges = securityUtils.getPrivileges(user.getId());
        boolean l1 = Boolean.TRUE.equals(privileges.get("L1Approval"));
        boolean l2 = Boolean.TRUE.equals(privileges.get("L2Approval"));
        if (l1 && l2) {
            return "l1_approver,l2_approver";
        }
        if (l1) {
            return "l1_approver";
        }
        if (l2) {
            return "l2_approver";
        }
        return null;
    }

    private static boolean visibleRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("admin")
                || normalized.equals("recruiter")
                || normalized.equals("committee_member");
    }

    private static String bearer(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "HRMS access token is required.");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "HRMS access token is required.");
        }
        return token;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record SessionIdentity(String email, String empCode) {
    }
}
