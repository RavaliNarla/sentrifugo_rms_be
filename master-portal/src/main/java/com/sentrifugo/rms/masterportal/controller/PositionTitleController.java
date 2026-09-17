package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.masterportal.dto.PositionTitleDTO;
import com.sentrifugo.rms.masterportal.service.PositionTitleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PositionTitleDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll(), "Position titles fetched successfully"));
    }

    @Operation(summary = "Position titles under a department - drives the Add Position screen's department-filtered dropdown")
    @GetMapping("/by-department/{departmentId}")
    public ResponseEntity<ApiResponse<List<PositionTitleDTO>>> getByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getByDepartment(departmentId), "Position titles fetched successfully"));
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
