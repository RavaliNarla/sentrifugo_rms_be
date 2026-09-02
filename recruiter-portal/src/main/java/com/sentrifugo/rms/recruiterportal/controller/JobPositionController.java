package com.sentrifugo.rms.recruiterportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.JobPositionDTO;
import com.sentrifugo.rms.recruiterportal.service.JobPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Job Positions")
@RestController
@RequestMapping("${recruiter.api.base.path}/job-positions")
@RequiredArgsConstructor
public class JobPositionController {

    private final JobPositionService jobPositionService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Create a position under a requisition (multipart: 'position' JSON part + optional 'approvalDoc' file)")
    @PostMapping(value = "/create", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<JobPositionDTO>> create(
            @RequestPart("position") String positionJson,
            @RequestPart(value = "approvalDoc", required = false) MultipartFile approvalDoc) throws Exception {
        JobPositionDTO dto = objectMapper.readValue(positionJson, JobPositionDTO.class);
        return ResponseEntity.ok(ApiResponse.ok(jobPositionService.create(dto, approvalDoc), "Position created successfully"));
    }

    @Operation(summary = "Update a position (multipart: 'position' JSON part + optional 'approvalDoc' file)")
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<JobPositionDTO>> update(
            @PathVariable UUID id,
            @RequestPart("position") String positionJson,
            @RequestPart(value = "approvalDoc", required = false) MultipartFile approvalDoc) throws Exception {
        JobPositionDTO dto = objectMapper.readValue(positionJson, JobPositionDTO.class);
        return ResponseEntity.ok(ApiResponse.ok(jobPositionService.update(id, dto, approvalDoc), "Position updated successfully"));
    }

    @Operation(summary = "Delete a position")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        jobPositionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Position deleted successfully"));
    }

    @Operation(summary = "List all positions under a requisition")
    @GetMapping("/by-requisition/{requisitionId}")
    public ResponseEntity<ApiResponse<List<JobPositionDTO>>> getByRequisition(@PathVariable UUID requisitionId) {
        return ResponseEntity.ok(ApiResponse.ok(jobPositionService.getByRequisitionId(requisitionId), "Positions fetched successfully"));
    }

    @Operation(summary = "List ACTIVE positions under a requisition - for the Candidate Workflow dropdown")
    @GetMapping("/active-by-requisition/{requisitionId}")
    public ResponseEntity<ApiResponse<List<JobPositionDTO>>> getActiveByRequisition(@PathVariable UUID requisitionId) {
        return ResponseEntity.ok(ApiResponse.ok(jobPositionService.getActiveByRequisitionId(requisitionId), "Active positions fetched successfully"));
    }

    @Operation(summary = "Get a position by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobPositionDTO>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(jobPositionService.getById(id), "Position fetched successfully"));
    }
}
