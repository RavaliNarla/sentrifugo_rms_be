package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.masterportal.dto.PositionTitleDTO;
import com.sentrifugo.rms.masterportal.service.PositionTitleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Position Titles")
@RestController
@RequestMapping("${master.api.base.path}/position-titles")
@RequiredArgsConstructor
public class PositionTitleController {

    private final PositionTitleService service;

    @Operation(summary = "Paginated list - syncs designations from HRMS when X-Hrms-Authorization is present")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<PositionTitleDTO>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-Hrms-Authorization", required = false) String hrmsAuthorization) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getAll(search, page, size, hrmsAuthorization), "Position titles fetched successfully"));
    }

    @Operation(summary = "Titles for a department including unlinked (department None) titles")
    @GetMapping("/by-department/{departmentId}")
    public ResponseEntity<ApiResponse<List<PositionTitleDTO>>> getByDepartment(
            @PathVariable UUID departmentId,
            @RequestHeader(value = "X-Hrms-Authorization", required = false) String hrmsAuthorization) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getByDepartment(departmentId, hrmsAuthorization), "Position titles fetched successfully"));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<PositionTitleDTO>> add(@Valid @RequestBody PositionTitleDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.add(dto), "Position title added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<PositionTitleDTO>> update(@PathVariable UUID id, @Valid @RequestBody PositionTitleDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, dto), "Position title updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Position title deleted successfully"));
    }
}
