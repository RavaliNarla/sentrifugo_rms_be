package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.db.entity.AttendanceEntryEntity;
import com.sentrifugo.rms.db.repository.AttendanceEntryRepository;
import com.sentrifugo.rms.recruiterportal.dto.AttendanceEntryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceEntryService {

    private final AttendanceEntryRepository attendanceEntryRepository;

    @Value("${app.attendance.account-id:rms_dev}")
    private String expectedAccountId;

    @Value("${app.attendance.password:Sagarsoft@12345}")
    private String expectedPassword;

    /**
     * Validates the {@code Authorization: Basic base64(accountId:password)} header,
     * then persists the attendance entry. Credentials are never stored.
     */
    @Transactional
    public Map<String, Object> record(String authorizationHeader, AttendanceEntryRequest request) {
        validateBasicAuth(authorizationHeader);

        String empCode = request.getEmpCode().trim();
        if (empCode.isEmpty()) {
            throw new CommonException("empCode is required.");
        }

        AttendanceEntryEntity saved = attendanceEntryRepository.save(AttendanceEntryEntity.builder()
                .empCode(empCode)
                .enteredAt(request.getTimestamp())
                .entryType(request.getType())
                .build());

        return Map.of(
                "id", saved.getId(),
                "empCode", saved.getEmpCode(),
                "timestamp", saved.getEnteredAt().toString(),
                "type", saved.getEntryType().name()
        );
    }

    /**
     * Parses {@code Authorization: Basic <base64>} where the decoded value is {@code accountId:password}.
     * Uses constant-time comparison to avoid timing attacks.
     */
    private void validateBasicAuth(String header) {
        final ResponseStatusException unauthorized =
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing Authorization header.");

        if (header == null || !header.startsWith("Basic ")) {
            throw unauthorized;
        }

        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw unauthorized;
        }

        int colon = decoded.indexOf(':');
        if (colon < 1) {
            throw unauthorized;
        }

        String accountId = decoded.substring(0, colon);
        String password = decoded.substring(colon + 1);

        if (!constantEquals(expectedAccountId, accountId)
                || !constantEquals(expectedPassword, password)) {
            throw unauthorized;
        }
    }

    private static boolean constantEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
