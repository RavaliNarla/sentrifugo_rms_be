package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.ApprovalActionRequest;
import com.sentrifugo.rms.recruiterportal.dto.JobRequisitionDTO;
import com.sentrifugo.rms.recruiterportal.service.JobRequisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Job Requisitions")
@RestController
@RequestMapping("${recruiter.api.base.path}/job-requisitions")
@RequiredArgsConstructor
public class JobRequisitionController {

    private final JobRequisitionService jobRequisitionService;

    @Operation(summary = "Create a new requisition (status NEW)")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<JobRequisitionDTO>> create(@Valid @RequestBody JobRequisitionDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.create(dto), "Requisition created successfully"));
    }

    @Operation(summary = "Update a requisition (only while NEW/REJECTED)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobRequisitionDTO>> update(@PathVariable UUID id, @Valid @RequestBody JobRequisitionDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.update(id, dto), "Requisition updated successfully"));
    }

    @Operation(summary = "Get a requisition by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobRequisitionDTO>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.getById(id), "Requisition fetched successfully"));
    }

    @Operation(summary = "List all requisitions (paginated), newest first")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobRequisitionDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.getAll(page, size), "Requisitions fetched successfully"));
    }

    @Operation(summary = "List APPROVED requisitions - for the Candidate Workflow dropdown")
    @GetMapping("/approved")
    public ResponseEntity<ApiResponse<List<JobRequisitionDTO>>> getApproved() {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.getApprovedForDropdown(), "Approved requisitions fetched successfully"));
    }

    @Operation(summary = "L1 approval queue")
    @GetMapping("/l1-requisitions")
    public ResponseEntity<ApiResponse<List<JobRequisitionDTO>>> getForL1() {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.getForL1Approval(), "L1 requisitions fetched successfully"));
    }

    @Operation(summary = "L2 approval queue")
    @GetMapping("/l2-requisitions")
    public ResponseEntity<ApiResponse<List<JobRequisitionDTO>>> getForL2() {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.getForL2Approval(), "L2 requisitions fetched successfully"));
    }

    @Operation(summary = "Submit one or more requisitions for L1 approval")
    @PostMapping("/submit-for-approval")
    public ResponseEntity<ApiResponse<Void>> submitForApproval(@RequestBody List<UUID> requisitionIds) {
        jobRequisitionService.submitForApproval(requisitionIds);
        return ResponseEntity.ok(ApiResponse.ok("Requisition(s) submitted for approval"));
    }

    @Operation(summary = "L1 or L2 approve/reject requisitions (role resolved from the caller's approver record)")
    @PostMapping("/approve-reject")
    public ResponseEntity<ApiResponse<Void>> approveOrReject(@RequestBody ApprovalActionRequest request) {
        jobRequisitionService.approveOrReject(request);
        return ResponseEntity.ok(ApiResponse.ok(request.isApprove() ? "Requisition(s) approved" : "Requisition(s) rejected"));
    }

    @Operation(summary = "Mark an approved requisition as Fulfilled")
    @PostMapping("/{id}/fulfil")
    public ResponseEntity<ApiResponse<Void>> markFulfilled(@PathVariable UUID id) {
        jobRequisitionService.markFulfilled(id);
        return ResponseEntity.ok(ApiResponse.ok("Requisition marked as fulfilled"));
    }
}
