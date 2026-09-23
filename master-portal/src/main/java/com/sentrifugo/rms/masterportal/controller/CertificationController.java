package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.masterportal.dto.CertificationDTO;
import com.sentrifugo.rms.masterportal.service.CertificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Certifications")
@RestController
@RequestMapping("${master.api.base.path}/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService service;

    @Operation(summary = "Full unpaginated list - used to populate the Add Position Certifications dropdown")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CertificationDTO>>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll(search), "Certifications fetched successfully"));
    }

    @Operation(summary = "Paginated + searchable list - drives the Certifications admin screen")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<CertificationDTO>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.search(search, page, size), "Certifications fetched successfully"));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CertificationDTO>> add(@Valid @RequestBody CertificationDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.add(dto), "Certification added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<CertificationDTO>> update(@PathVariable UUID id, @Valid @RequestBody CertificationDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, dto), "Certification updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Certification deleted successfully"));
    }
}
