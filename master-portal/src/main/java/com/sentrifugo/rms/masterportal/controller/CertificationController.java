package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.masterportal.dto.CertificationDTO;
import com.sentrifugo.rms.masterportal.service.CertificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CertificationDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll(), "Certifications fetched successfully"));
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
