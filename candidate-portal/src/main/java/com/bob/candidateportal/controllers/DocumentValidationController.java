package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.DocumentValidationService;
import com.bob.candidateportal.model.BirthdateProofUploadRequest;
import com.bob.candidateportal.model.IdProofUploadRequest;
import com.bob.candidateportal.model.DocumentValidationResult;
import com.bob.candidateportal.model.WorkExpUploadRequest;
import com.bob.db.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${candidate.api.base.path}/validate-document")
public class DocumentValidationController {

    @Autowired
    private DocumentValidationService documentValidationService;

    @PostMapping(value = "/validate/{certType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> validateCertificate(
            @RequestParam("file") MultipartFile file,
            @PathVariable String certType) {

            boolean isValid = documentValidationService.validateCertificate(file, certType);
            if (isValid) {
                return ResponseEntity.ok(ApiResponse.ok("Certificate validated successfully", "Validation Successful"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("Certificate validation failed. Not a valid "+certType+" document"));
            }

    }

    @PostMapping(value = "/validate-document/{docCode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Boolean>> validateDocument(
            @RequestParam("file") MultipartFile file,
            @PathVariable String docCode) {
            boolean isValid = documentValidationService.validateDocument(file, docCode);
            if (isValid) {
                return ResponseEntity.ok(ApiResponse.ok(isValid, "Document validated successfully"));
            } else {
                return ResponseEntity.ok(ApiResponse.ok(isValid, "Document validation failed. Keywords not found in the uploaded file."));
            }
    }

    @PostMapping(value = "/validate-id-proof-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentValidationResult>> validateIdProofDocument(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") IdProofUploadRequest request) {
        try {
            DocumentValidationResult result = documentValidationService.validateIdProofDocument(
                    file, request.getDocumentId(), request.getDocumentNumber());
            
            String message;
            if ("validated".equals(result.getStatus())) {
                message = "ID proof document validated successfully.";
            } else if ("pending".equals(result.getStatus())) {
                message = "Document is pending validation.";
            } else {
                message = "Document is invalid.";
            }
            
            return ResponseEntity.ok(ApiResponse.ok(result, message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping(value = "/validate-birthdate-proof-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentValidationResult>> validateBirthdateProofDocument(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") BirthdateProofUploadRequest request) {
        try {
            DocumentValidationResult result = documentValidationService.validateBirthdateProofDocument(
                    file, request.getDocumentId(), request.getFullNameAsPerCertificate(), request.getDateOfBirth());
            
            String message;
            if ("validated".equals(result.getStatus())) {
                message = "Birthdate proof document validated successfully - document is valid and date of birth matched.";
            } else if ("pending".equals(result.getStatus())) {
                message = "Birthdate proof document pending validation - document is valid but date of birth does not match.";
            } else {
                message = "Birthdate proof validation rejected - invalid document.";
            }
            
            return ResponseEntity.ok(ApiResponse.ok(result, message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping(value = "/validate-work-exp-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentValidationResult>> validateWorkExpDocument(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") WorkExpUploadRequest request) {
        try {
            DocumentValidationResult result = documentValidationService.validateWorkExpDocument(
                    file, request.getDocumentId(), request.getOrganization(), request.getRole(), 
                    request.getPostHeld(), request.getFromDate(), request.getToDate(), request.getIsPresentlyWorking());
            
            String message;
            if ("validated".equals(result.getStatus())) {
                message = "Work experience document validated successfully.";
            } else if ("pending".equals(result.getStatus())) {
                message = "Document is pending validation.";
            } else {
                message = "Document is invalid.";
            }
            
            return ResponseEntity.ok(ApiResponse.ok(result, message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
