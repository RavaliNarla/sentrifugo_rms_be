package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CandidateDocumentsService;
import com.bob.candidateportal.model.DocumentUploadRequest;
import com.bob.candidateportal.model.IdProofUploadRequest;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CandidateDocumentStoreDTO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/documents")
public class CandidateDocumentsController {

    @Autowired
    private CandidateDocumentsService candidateDocumentsService;
    @Autowired
    private SecurityUtils securityUtils;

    @RequestBody(
            content = @Content(
                    encoding = @Encoding(name = "request", contentType = "application/json")
            )
    )
    @PostMapping(value = "/upload-document/{documentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadCandidateDocumentWithValidation(
            @PathVariable UUID documentId,
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") DocumentUploadRequest request) throws Exception {
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateDocumentStoreDTO candidateDocumentStoreDTO = candidateDocumentsService.uploadCandidateDocumentWithValidation(
                candidateId, documentId, request.getIsValidationPending(), request.getPendingChecks(), file);
        return ResponseEntity.ok(ApiResponse.ok(candidateDocumentStoreDTO, "Candidate document uploaded successfully"));
    }

    @PostMapping(value ="/upload-other/{docName}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadOtherCandidateDocuments( @PathVariable String docName, @RequestParam("file") MultipartFile documents){
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateDocumentStoreDTO candidateDocumentStoreDTO = candidateDocumentsService.uploadOtherCandidateDocuments(candidateId,docName, documents);
        return ResponseEntity.ok(ApiResponse.ok(candidateDocumentStoreDTO, "Candidate documents uploaded successfully"));
    }

    @GetMapping("/get-doc")
    public ResponseEntity<ApiResponse<?>> getCandidateDocuments(){
        UUID candidateId = securityUtils.getCurrentUserId();
        List<CandidateDocumentStoreDTO> res = candidateDocumentsService.getCandidateDocuments(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate documents fetched successfully"));
    }
    @GetMapping("/get-doc-by-doccode/{docCode}")
    public ResponseEntity<ApiResponse<?>> getCandidateDocumentsByDocCode(@PathVariable String docCode){
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateDocumentStoreDTO res = candidateDocumentsService.getCandidateDocumentsByDocCode(candidateId,docCode);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate documents fetched successfully"));
    }

    @DeleteMapping("/delete-doc/{documentId}")
    public ResponseEntity<ApiResponse<?>> deleteCandidateDocuments( @PathVariable UUID documentId){
        UUID candidateId = securityUtils.getCurrentUserId();
        candidateDocumentsService.deleteCandidateDocuments(candidateId, documentId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Candidate documents deleted successfully"));
    }

    @RequestBody(
            content = @Content(
                    encoding = @Encoding(name = "request", contentType = "application/json")
            )
    )
    @PostMapping(value = "/upload-idproof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadIdProofDocument(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") IdProofUploadRequest request){
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateDocumentStoreDTO candidateDocumentStoreDTO = candidateDocumentsService.uploadIdProofDocument(
                candidateId, request.getDocumentId(), request.getDocumentNumber(),
                request.getIsValidationPending(), request.getPendingChecks(), file, false);
        return ResponseEntity.ok(ApiResponse.ok(candidateDocumentStoreDTO, "ID Proof document uploaded successfully"));
    }
}
