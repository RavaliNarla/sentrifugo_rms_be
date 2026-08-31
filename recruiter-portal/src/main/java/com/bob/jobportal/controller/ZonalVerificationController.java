package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CandidateApplicationDocumentVerificationDTO;
import com.bob.jobportal.mapper.ZonalDocumentVerificationMapper;
import com.bob.jobportal.model.BatchAbsentStatusRequest;
import com.bob.jobportal.model.ZonalDocumentVerificationRequest;
import com.bob.jobportal.model.ZonalOverallVerificationRequest;
import com.bob.jobportal.model.ZonalVerificationResponseModel;
import com.bob.jobportal.service.DocumentVerificationService;
import com.bob.jobportal.service.ZonalVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/zonal-verification")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
@Tag(name = "Zonal Verification", description = "APIs for Zonal HR to verify candidate documents at interview centers")
@Slf4j
public class ZonalVerificationController {

    @Autowired
    private ZonalVerificationService zonalVerificationService;

    @Autowired
    private DocumentVerificationService documentVerificationService;

    @Autowired
    private ZonalDocumentVerificationMapper zonalDocumentVerificationMapper;

    @GetMapping("/candidates")
    @Operation(
            summary = "Get candidates for zonal verification by interview date",
            description = "Retrieves all candidates scheduled for interview at the logged-in user's interview center on the specified date. " +
                    "Only shows candidates with SCHEDULED or RESCHEDULED status."
    )
    public ResponseEntity<ApiResponse<List<ZonalVerificationResponseModel>>> getCandidatesForVerification(
            @Parameter(description = "Interview date (format: yyyy-MM-dd)", required = true, example = "2025-11-24")
            @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate date
    ) {
        log.info("Fetching candidates for zonal verification for date: {}", date);
        
        List<ZonalVerificationResponseModel> candidates = 
                zonalVerificationService.getCandidatesForZonalVerification(date);
        
        return ResponseEntity.ok(
                ApiResponse.ok(candidates, 
                        String.format("Found %d candidates scheduled for verification on %s", 
                                candidates.size(), date))
        );
    }

    @PostMapping("/update-absent-status")
    @Operation(
            summary = "Update candidate absent status",
            description = "Updates the is_absent flag for a candidate application. " +
                    "Set true if candidate is absent, false if present."
    )
    public ResponseEntity<ApiResponse<Void>> updateAbsentStatus(
            @RequestParam UUID applicationId,
            @RequestParam Boolean isAbsent
    ) {
        log.info("Updating absent status for application: {}, isAbsent: {}", 
                applicationId, isAbsent);
        
        zonalVerificationService.updateAbsentStatus(applicationId, isAbsent);
        
        return ResponseEntity.ok(
                ApiResponse.ok(null, "Absent status updated successfully")
        );
    }

    @PostMapping("/update-absent-statuses")
    @Operation(
            summary = "Update multiple candidates' absent status in batch",
            description = "Updates the is_absent flag for multiple candidate applications in a single request. " +
                    "This is more efficient than calling the single update endpoint multiple times. " +
                    "Set isAbsent to true if candidate is absent, false if present."
    )
    public ResponseEntity<ApiResponse<Void>> updateBatchAbsentStatus(
            @Valid @RequestBody BatchAbsentStatusRequest request
    ) {
        log.info("Updating absent status for {} applications in batch", 
                request.getAbsentStatusUpdates().size());
        
        zonalVerificationService.updateBatchAbsentStatus(request.getAbsentStatusUpdates());
        
        return ResponseEntity.ok(
                ApiResponse.ok(null, 
                        String.format("Successfully updated absent status for %d candidates",
                                request.getAbsentStatusUpdates().size()))
        );
    }

    @GetMapping("/documents/{applicationId}")
    @Operation(
            summary = "Get all documents for zonal verification",
            description = "Retrieves all documents for the given application to be verified by Zonal HR"
    )
    public ResponseEntity<ApiResponse<List<CandidateApplicationDocumentVerificationDTO>>> getDocumentsForVerification(
            @PathVariable UUID applicationId
    ) {
        log.info("Fetching documents for zonal verification, application: {}", applicationId);
        
        // Reuse existing screening service - returns all document records including zonal columns
        List<CandidateApplicationDocumentVerificationDTO> documents = 
                documentVerificationService.getDocumentScreeningDetails(applicationId);
        
        return ResponseEntity.ok(
                ApiResponse.ok(documents, "Documents fetched successfully")
        );
    }

    @PostMapping("/verify-document")
    @Operation(
            summary = "Verify individual document",
            description = "Updates zonal HR verification status for a specific document. " +
                    "Reuses existing document verification service with role-based logic."
    )
    public ResponseEntity<ApiResponse<CandidateApplicationDocumentVerificationDTO>> verifyDocument(
            @Valid @RequestBody ZonalDocumentVerificationRequest request
    ) {
        log.info("Zonal HR verifying document: {}, status: {}", 
                request.getCandidateDocumentId(), request.getZonalHrDocStatus());
        
        // Use MapStruct mapper to convert request to DTO
        CandidateApplicationDocumentVerificationDTO dto = zonalDocumentVerificationMapper.toDTO(request);
        
        // Use shared service - it will detect Zonal_HR role and update zonal columns
        CandidateApplicationDocumentVerificationDTO updated = 
                documentVerificationService.saveDocumentScreeningDetails(dto);
        
        return ResponseEntity.ok(
                ApiResponse.ok(updated, "Document verification status updated successfully")
        );
    }

    @PostMapping("/submit-overall-verification")
    @Operation(
            summary = "Submit overall zonal verification",
            description = "Updates overall verification status for the candidate after all documents are verified. " +
                    "Maps UI values: Yes -> VERIFIED, No -> REJECTED, Provisionally Approved -> PROVISIONALLY_APPROVED"
    )
    public ResponseEntity<ApiResponse<Void>> submitOverallVerification(
            @Valid @RequestBody ZonalOverallVerificationRequest request
    ) {
        log.info("Submitting overall zonal verification for application: {}, status: {}", 
                request.getApplicationId(), request.getZonalVerificationStatus());
        
        zonalVerificationService.submitOverallVerification(request);
        
        return ResponseEntity.ok(
                ApiResponse.ok(null, "Overall verification submitted successfully")
        );
    }
}
