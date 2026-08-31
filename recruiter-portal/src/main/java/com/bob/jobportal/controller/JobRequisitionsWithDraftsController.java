package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.enums.RequisitionStatus;
import com.bob.jobportal.model.JobRequisitionWithDraftResponseModel;
import com.bob.jobportal.service.JobRequisitionsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/job-requisitions-with-drafts")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER','COMMITEE_MEMBER', 'COMMITTEE_MEMBER')")
@Validated
public class JobRequisitionsWithDraftsController {

    @Autowired
    private JobRequisitionsService jobRequisitionsService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobRequisitionWithDraftResponseModel>>> searchRequisitionsWithDrafts(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) RequisitionStatus status,
            @RequestParam(required = false) @Size(max = 100, message = "Search term cannot exceed 100 characters") String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number cannot be negative") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 50, message = "Page size cannot exceed 50") int size
    ) {
        if (size > 50) {
            size = 50;
        }

        Page<JobRequisitionWithDraftResponseModel> requisitions = jobRequisitionsService.searchRequisitionsWithDrafts(
                year, month, status != null ? List.of(status) : null, search, departmentId, page, size
        );
        return ResponseEntity.ok(ApiResponse.ok(requisitions, "Requisitions with drafts fetched successfully"));
    }
}
