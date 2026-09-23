package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.masterportal.dto.NamedMasterDTO;
import com.sentrifugo.rms.masterportal.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Departments")
@RestController
@RequestMapping("${master.api.base.path}/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Full unpaginated list - used to populate dropdowns elsewhere")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<NamedMasterDTO>>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.getAll(search), "Departments fetched successfully"));
    }

    @Operation(summary = "Paginated + searchable list - drives the Departments admin screen")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<NamedMasterDTO>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.search(search, page, size), "Departments fetched successfully"));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<NamedMasterDTO>> add(@Valid @RequestBody NamedMasterDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.add(dto), "Department added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<NamedMasterDTO>> update(@PathVariable UUID id, @Valid @RequestBody NamedMasterDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.update(id, dto), "Department updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully"));
    }
}
