package com.bob.jobportal.controller;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.JobRequisitionsDTO;
import com.bob.db.enums.RequisitionStatus;
import com.bob.jobportal.model.CancelRequisitionEditDraftRequestModel;
import com.bob.jobportal.model.CreateRequisitionEditDraftRequestModel;
import com.bob.jobportal.model.JobPostingRequestModel;
import com.bob.jobportal.model.JobRequisitionWithDraftResponseModel;
import com.bob.jobportal.model.RequisitionReinitializeRequestModel;
import com.bob.jobportal.model.RequisitionEditDraftResponseModel;
import com.bob.jobportal.model.ApproveRequisitionEditDraftRequestModel;
import com.bob.jobportal.model.SubmitRequisitionEditDraftRequestModel;
import com.bob.jobportal.model.UpdatePositionEditDraftRequestModel;
import com.bob.jobportal.model.UpdateRequisitionEditDraftRequestModel;
import com.bob.jobportal.service.JobRequisitionsService;
import jakarta.mail.MessagingException;
import com.bob.jobportal.service.RequisitionEditDraftService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/job-requisitions")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER','COMMITEE_MEMBER', 'COMMITTEE_MEMBER')")
@Validated
public class JobRequisitionsController {

    @Autowired
    private JobRequisitionsService jobRequisitionsService;

    @Autowired
    private RequisitionEditDraftService requisitionEditDraftService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Void>> createJobRequisition(
            @Valid @RequestBody JobRequisitionsDTO requisition
    ){

        jobRequisitionsService.createJobRequisition(requisition);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(null, "Job requisition created successfully"));
    }

    @PostMapping("/reinitialize")
    public ResponseEntity<ApiResponse<JobRequisitionsDTO>> reinitializeRequisition(@Valid @RequestBody RequisitionReinitializeRequestModel request) {
        JobRequisitionsDTO created = jobRequisitionsService.reinitializeRequisition(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Requisition reinitialized successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobRequisitionsDTO>>> searchRequisitions(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) RequisitionStatus status,
            @RequestParam(required = false) @Size(max = 100, message = "Search term cannot exceed 100 characters") String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number cannot be negative") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 50, message = "Page size cannot exceed 50") int size
    ) {
        // Enforce pagination cap (defense in depth)
        if (size > 50) {
            size = 50;
        }
        
        Page<JobRequisitionsDTO> requisitions = jobRequisitionsService.searchRequisitions(year, month,status != null ? List.of(status) : null, search,departmentId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(requisitions, "Requisitions fetched successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobRequisitionsDTO>> getRequisitionById(
            @PathVariable UUID id
    ) {
        JobRequisitionsDTO requisition = jobRequisitionsService.getRequisitionById(id);
        return ResponseEntity.ok(ApiResponse.ok(requisition, "Requisition fetched successfully"));
    }

    @PostMapping("/{id}/edit-drafts")
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> createOrGetEditDraft(
            @PathVariable UUID id,
            @RequestBody(required = false) CreateRequisitionEditDraftRequestModel request
    ) {
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.createOrGetDraft(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Requisition edit draft ready"));
    }

    @GetMapping("/{id}/edit-drafts/current")
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> getCurrentEditDraft(
            @PathVariable UUID id
    ) {
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.getCurrentDraft(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Current requisition edit draft fetched successfully"));
    }

    @PutMapping("/{id}/edit-drafts/current")
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> updateCurrentEditDraft(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRequisitionEditDraftRequestModel request
    ) {
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.updateRequisitionDraft(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Requisition edit draft updated successfully"));
    }

    @PutMapping(value = "/{id}/edit-drafts/current/positions/{positionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> upsertPositionEditDraft(
            @PathVariable UUID id,
            @PathVariable UUID positionId,
            @RequestBody UpdatePositionEditDraftRequestModel request
    ) {
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.upsertPositionDraft(id, positionId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Position draft updated successfully"));
    }

    @PutMapping(value = "/{id}/edit-drafts/current/positions/{positionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> upsertPositionEditDraftWithFile(
            @PathVariable UUID id,
            @PathVariable UUID positionId,
            @RequestPart(value = "indentFile", required = false) MultipartFile indentFile,
            @RequestPart(value = "request", required = false) UpdatePositionEditDraftRequestModel request,
            @RequestPart(value = "jobPositionsDTO", required = false) UpdatePositionEditDraftRequestModel legacyRequest
    ) {
        UpdatePositionEditDraftRequestModel effectiveRequest = request != null ? request : legacyRequest;
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.upsertPositionDraft(id, positionId, effectiveRequest, indentFile);
        return ResponseEntity.ok(ApiResponse.ok(response, "Position draft updated successfully"));
    }

    /**
     * Direct approval mode that skips L1/L2 and marks the draft approved immediately.
     * Frontend can use this endpoint when direct mode is selected.
     */
    @PostMapping("/{id}/edit-drafts/current/submit-for-approval")
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> submitEditDraftForApproval(
            @PathVariable UUID id,
            @RequestBody(required = false) SubmitRequisitionEditDraftRequestModel request
    ) {
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.submitForApprovalDirect(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Requisition edit draft approved successfully"));
    }

    @PostMapping("/{id}/edit-drafts/current/submit-for-approval-new")
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> submitEditDraftForApprovalNew(
            @PathVariable UUID id,
            @RequestBody(required = false) SubmitRequisitionEditDraftRequestModel request
    ) {
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.submitForApprovalWorkflow(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Requisition edit draft submitted for L1 approval"));
    }

    @PostMapping("/{id}/edit-drafts/current/approve")
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> approveEditDraft(
            @PathVariable UUID id,
            @RequestBody ApproveRequisitionEditDraftRequestModel request
    ) {
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.approveDraftWithWorkflow(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Requisition edit draft approval action completed"));
    }

    @PostMapping("/{id}/edit-drafts/current/publish")
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> publishEditDraft(
            @PathVariable UUID id
    ) {
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.publishApprovedDraft(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Requisition edit draft published successfully"));
    }

    @PostMapping("/{id}/edit-drafts/current/cancel")
    public ResponseEntity<ApiResponse<RequisitionEditDraftResponseModel>> cancelEditDraft(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelRequisitionEditDraftRequestModel request
    ) {
        RequisitionEditDraftResponseModel response = requisitionEditDraftService.cancelDraft(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Requisition edit draft cancelled"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateJobRequisition(
            @PathVariable UUID id,
            @Valid @RequestBody JobRequisitionsDTO requisition
    ){

        jobRequisitionsService.updateJobRequisition(id, requisition);

        return ResponseEntity.ok(ApiResponse.ok(null, "Job requisition updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJobRequisition(
            @PathVariable UUID id
    ) {
        jobRequisitionsService.deleteJobRequisition(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Job requisition deleted successfully"));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<Void>> closeJobRequisition(
            @PathVariable UUID id
    ) {
        jobRequisitionsService.closeRequisition(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Job requisition closed successfully"));
    }

    @PostMapping("/approve-job-requisitions")
    public ResponseEntity<ApiResponse<List<JobRequisitionWithDraftResponseModel>>> approveJobRequisitions(@Valid @RequestBody JobPostingRequestModel jobPostingRequestModel) throws MessagingException, IOException {
        if(jobPostingRequestModel.getJobRequisitionIds() == null){
            return ResponseEntity.badRequest().body(ApiResponse.error(AppConstants.REQUISITION_ID_REQUIRED_MESSAGE));
        }
        List<JobRequisitionWithDraftResponseModel> result = jobRequisitionsService.approveJobRequisitions(jobPostingRequestModel);
        return ResponseEntity.ok(ApiResponse.ok(result, "Job Requisition approval action completed successfully"));
    }

    @PostMapping("/submit-for-approval")
    public ResponseEntity<ApiResponse<List<JobRequisitionsDTO>>> submitForApproval(@Valid @RequestBody JobPostingRequestModel jobPostingRequestModel){
        if(jobPostingRequestModel.getJobRequisitionIds() == null){
            return ResponseEntity.badRequest().body(ApiResponse.error(AppConstants.REQUISITION_ID_REQUIRED_MESSAGE));
        }
        List<JobRequisitionsDTO> jobRequisitionsDTOS= jobRequisitionsService.submitForApprovalOld(jobPostingRequestModel);
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionsDTOS,"Job Requisition submitted by recruiter successfully"));
    }

    @PostMapping("/submit-for-approval-new")
    public ResponseEntity<ApiResponse<List<JobRequisitionWithDraftResponseModel>>> submitForApprovalNew(@Valid @RequestBody JobPostingRequestModel jobPostingRequestModel) throws MessagingException, IOException {
        if(jobPostingRequestModel.getJobRequisitionIds() == null){
            return ResponseEntity.badRequest().body(ApiResponse.error(AppConstants.REQUISITION_ID_REQUIRED_MESSAGE));
        }
        List<JobRequisitionWithDraftResponseModel> result = jobRequisitionsService.submitForApproval(jobPostingRequestModel);
        return ResponseEntity.ok(ApiResponse.ok(result, "Job Requisition submitted for approval successfully"));
    }


    @GetMapping("/get-requisitions")
    public ResponseEntity<ApiResponse<List<JobRequisitionsDTO>>> getApprovedRequisitionsByName(
            @RequestParam(required = false) @Size(max = 100, message = "Search text cannot exceed 100 characters") String searchText){
        List<JobRequisitionsDTO> jobRequisitionsDTOS= jobRequisitionsService.getApprovedRequisitionsByName(searchText);
        return ResponseEntity.ok(ApiResponse.ok(jobRequisitionsDTOS,"Job Requisitions fetched successfully"));
    }

    @GetMapping("/get-years")
    public ResponseEntity<ApiResponse<List<Integer>>> getYears(){
        List<Integer> years= jobRequisitionsService.getYears();
        return ResponseEntity.ok(ApiResponse.ok(years,"Years fetched successfully"));
    }

    @GetMapping("/l1-requisitions")
    public ResponseEntity<ApiResponse<Page<JobRequisitionWithDraftResponseModel>>> getL1Requisitions(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @Size(max = 100, message = "Search term cannot exceed 100 characters") String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) List<RequisitionStatus> statuses,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number cannot be negative") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 50, message = "Page size cannot exceed 50") int size
    ) {
        if (size > 50) {
            size = 50;
        }
        List<RequisitionStatus> l1Statuses = List.of(RequisitionStatus.L1_PENDING, RequisitionStatus.L2_PENDING, RequisitionStatus.L1_REJECTED, RequisitionStatus.APPROVED, RequisitionStatus.L2_REJECTED);
        if (statuses != null && !statuses.isEmpty()) {
            List<RequisitionStatus> invalid = statuses.stream().filter(s -> !l1Statuses.contains(s)).toList();
            if (!invalid.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid statuses for L1: " + invalid));
            }
        }
        List<RequisitionStatus> effectiveStatuses = (statuses != null && !statuses.isEmpty()) ? statuses : l1Statuses;
        Page<JobRequisitionWithDraftResponseModel> requisitions = jobRequisitionsService.searchRequisitionsWithDrafts(year, month, effectiveStatuses, search, departmentId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(requisitions, "L1 Requisitions fetched successfully"));
    }

    @GetMapping("/l2-requisitions")
    public ResponseEntity<ApiResponse<Page<JobRequisitionWithDraftResponseModel>>> getL2Requisitions(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @Size(max = 100, message = "Search term cannot exceed 100 characters") String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) List<RequisitionStatus> statuses,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number cannot be negative") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 50, message = "Page size cannot exceed 50") int size
    ) {
        if (size > 50) {
            size = 50;
        }
        List<RequisitionStatus> l2Statuses = List.of(RequisitionStatus.L2_PENDING, RequisitionStatus.APPROVED, RequisitionStatus.L2_REJECTED);
        if (statuses != null && !statuses.isEmpty()) {
            List<RequisitionStatus> invalid = statuses.stream().filter(s -> !l2Statuses.contains(s)).toList();
            if (!invalid.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid statuses for L2: " + invalid));
            }
        }
        List<RequisitionStatus> effectiveStatuses = (statuses != null && !statuses.isEmpty()) ? statuses : l2Statuses;
        Page<JobRequisitionWithDraftResponseModel> requisitions = jobRequisitionsService.searchRequisitionsWithDrafts(year, month, effectiveStatuses, search, departmentId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(requisitions, "L2 Requisitions fetched successfully"));
    }

}
