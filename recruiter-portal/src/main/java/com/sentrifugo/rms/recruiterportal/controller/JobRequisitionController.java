package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.db.enums.RequisitionStatus;
import com.sentrifugo.rms.recruiterportal.dto.ApprovalActionRequest;
import com.sentrifugo.rms.recruiterportal.dto.JobRequisitionDTO;
import com.sentrifugo.rms.recruiterportal.dto.RequisitionFilterOptionsDTO;
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

    @Operation(summary = "Delete a requisition (only while status = NEW)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        jobRequisitionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Requisition deleted successfully"));
    }

    @Operation(summary = "List all requisitions (paginated), newest first")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobRequisitionDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.getAll(page, size), "Requisitions fetched successfully"));
    }

    @Operation(summary = "Search/filter requisitions for the Job Postings screen (server-side paginated), newest first")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<JobRequisitionDTO>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RequisitionStatus statusEnum = (status == null || status.isBlank()) ? null : RequisitionStatus.valueOf(status);
        return ResponseEntity.ok(ApiResponse.ok(
                jobRequisitionService.search(search, statusEnum, yearFrom, yearTo, month, jobTitle, department, location, page, size),
                "Requisitions fetched successfully"));
    }

    @Operation(summary = "Distinct filter values (years/job titles/departments/locations) for the Job Postings filter dropdowns")
    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<RequisitionFilterOptionsDTO>> filterOptions() {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.getFilterOptions(), "Filter options fetched successfully"));
    }

    @Operation(summary = "List APPROVED requisitions - for the Candidate Workflow dropdown")
    @GetMapping("/approved")
    public ResponseEntity<ApiResponse<List<JobRequisitionDTO>>> getApproved() {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.getApprovedForDropdown(), "Approved requisitions fetched successfully"));
    }

    @Operation(summary = "L1 approval queue (server-side search/status-filter, paginated)")
    @GetMapping("/l1-requisitions")
    public ResponseEntity<ApiResponse<Page<JobRequisitionDTO>>> getForL1(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RequisitionStatus statusEnum = (status == null || status.isBlank()) ? null : RequisitionStatus.valueOf(status);
        return ResponseEntity.ok(ApiResponse.ok(
                jobRequisitionService.searchForApproval(false, search, statusEnum, page, size), "L1 requisitions fetched successfully"));
    }

    @Operation(summary = "L2 approval queue (server-side search/status-filter, paginated)")
    @GetMapping("/l2-requisitions")
    public ResponseEntity<ApiResponse<Page<JobRequisitionDTO>>> getForL2(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RequisitionStatus statusEnum = (status == null || status.isBlank()) ? null : RequisitionStatus.valueOf(status);
        return ResponseEntity.ok(ApiResponse.ok(
                jobRequisitionService.searchForApproval(true, search, statusEnum, page, size), "L2 requisitions fetched successfully"));
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

    @Operation(summary = "Undo Mark Fulfilled - reopens the requisition back to APPROVED")
    @PostMapping("/{id}/unfulfil")
    public ResponseEntity<ApiResponse<Void>> unmarkFulfilled(@PathVariable UUID id) {
        jobRequisitionService.unmarkFulfilled(id);
        return ResponseEntity.ok(ApiResponse.ok("Requisition reopened successfully"));
    }

    @Operation(summary = "SCL_53: Approval history for a requisition (for the Job Postings history modal)")
    @GetMapping("/{id}/approval-history")
    public ResponseEntity<ApiResponse<List<com.sentrifugo.rms.recruiterportal.dto.RequisitionApprovalHistoryDTO>>> getApprovalHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionService.getApprovalHistory(id), "Approval history fetched successfully"));
    }
}
