package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.AttendanceEntryRequest;
import com.sentrifugo.rms.recruiterportal.service.AttendanceEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Unauthenticated attendance ingest for an external client system.
 * Covered by {@code /api/v1/public/**} in {@code AppConstants.PUBLIC_ENDPOINTS} — no JWT required.
 * Identity is verified via {@code Authorization: Basic base64(accountId:password)}.
 */
@Tag(name = "Public - Attendance")
@RestController
@RequestMapping("/api/v1/public/attendance")
@RequiredArgsConstructor
public class PublicAttendanceController {

    private final AttendanceEntryService attendanceEntryService;

    @Operation(summary = "Record one attendance entry (IN / OUT)",
               description = "Header: Authorization: Basic base64(accountId:password)")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> entry(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AttendanceEntryRequest request
    ) {
        Map<String, Object> data = attendanceEntryService.record(authorization, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Attendance recorded"));
    }
}
