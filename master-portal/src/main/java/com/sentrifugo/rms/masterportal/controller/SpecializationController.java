package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.masterportal.dto.SpecializationDTO;
import com.sentrifugo.rms.masterportal.service.SpecializationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Specializations")
@RestController
@RequestMapping("${master.api.base.path}/specializations")
@RequiredArgsConstructor
public class SpecializationController {

    private final SpecializationService service;

    @Operation(summary = "Paginated + searchable list - drives the Specializations admin screen")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<SpecializationDTO>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll(search, page, size), "Specializations fetched successfully"));
    }

    @Operation(summary = "Specializations for an education level (plus general/unlinked ones) - drives the optional Specialization dropdown on Add Position")
    @GetMapping("/by-education/{educationQualificationId}")
    public ResponseEntity<ApiResponse<List<SpecializationDTO>>> getByEducation(@PathVariable UUID educationQualificationId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getByEducation(educationQualificationId), "Specializations fetched successfully"));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<SpecializationDTO>> add(@Valid @RequestBody SpecializationDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.add(dto), "Specialization added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<SpecializationDTO>> update(@PathVariable UUID id, @Valid @RequestBody SpecializationDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, dto), "Specialization updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Specialization deleted successfully"));
    }
}
