package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.masterportal.dto.NamedMasterDTO;
import com.sentrifugo.rms.masterportal.service.EducationQualificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Education Qualifications")
@RestController
@RequestMapping("${master.api.base.path}/education-qualifications")
@RequiredArgsConstructor
public class EducationQualificationController {

    private final EducationQualificationService service;

    @Operation(summary = "Full unpaginated list - used to populate dropdowns elsewhere")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<NamedMasterDTO>>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll(search), "Education qualifications fetched successfully"));
    }

    @Operation(summary = "Paginated + searchable list - drives the Education Qualifications admin screen")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<NamedMasterDTO>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.search(search, page, size), "Education qualifications fetched successfully"));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<NamedMasterDTO>> add(@Valid @RequestBody NamedMasterDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.add(dto), "Education qualification added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<NamedMasterDTO>> update(@PathVariable UUID id, @Valid @RequestBody NamedMasterDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, dto), "Education qualification updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Education qualification deleted successfully"));
    }
}
