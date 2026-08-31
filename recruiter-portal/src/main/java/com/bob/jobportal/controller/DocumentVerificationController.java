package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CandidateApplicationDocumentVerificationDTO;
import com.bob.db.dto.CandidateDocumentStoreDTO;
import com.bob.jobportal.model.AddAdditionalRequiredDocumentRequest;
import com.bob.jobportal.service.DocumentVerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/document-verification")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class DocumentVerificationController {

    @Autowired
    private DocumentVerificationService documentVerificationService;

    @PostMapping("/save-screening-committee/verify")
    public ResponseEntity<ApiResponse<CandidateApplicationDocumentVerificationDTO>> verifyDocumentScreeningDetails(@Valid @RequestBody CandidateApplicationDocumentVerificationDTO candidateDocumentStore){
        CandidateApplicationDocumentVerificationDTO updatedCandidateDocumentStoreDTO = documentVerificationService.saveDocumentScreeningDetails(candidateDocumentStore);
        return new ResponseEntity<>(ApiResponse.ok(updatedCandidateDocumentStoreDTO,"Updated Screening Details Successfully"), HttpStatus.OK);

    }

    @GetMapping("/get-screening-committee/{applicationId}")
    public ResponseEntity<ApiResponse<List<CandidateApplicationDocumentVerificationDTO>>> getDocumentScreeningDetails(@PathVariable UUID applicationId){
        List<CandidateApplicationDocumentVerificationDTO> documentScreeningDetails = documentVerificationService.getDocumentScreeningDetails(applicationId);
        return new ResponseEntity<>(ApiResponse.ok(documentScreeningDetails,"Fetched Screening Details Successfully"), HttpStatus.OK);
    }

    @PostMapping("/add-additional-required-document")
    public ResponseEntity<ApiResponse<CandidateApplicationDocumentVerificationDTO>> addAdditionalRequiredDocument(
            @Valid @RequestBody AddAdditionalRequiredDocumentRequest request
    ) {
        CandidateApplicationDocumentVerificationDTO createdDoc = documentVerificationService.addAdditionalRequiredDocument(request);
        return new ResponseEntity<>(ApiResponse.ok(createdDoc, "Additional required document added successfully"), HttpStatus.OK);
    }

    @DeleteMapping("/additional-required-document/{verificationId}")
    public ResponseEntity<ApiResponse<String>> deleteAdditionalRequiredDocument(@PathVariable UUID verificationId) {
        documentVerificationService.deleteAdditionalRequiredDocument(verificationId);
        return new ResponseEntity<>(ApiResponse.ok("Deleted", "Additional required document deleted successfully"), HttpStatus.OK);
    }
}
