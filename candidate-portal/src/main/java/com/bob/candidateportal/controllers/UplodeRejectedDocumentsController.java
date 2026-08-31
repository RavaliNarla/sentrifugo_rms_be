package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.UplodeRejectedDocumentsService;
import com.bob.candidateportal.model.ScreeningAndZonalRejectedDocuments;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CandidateApplicationDocumentVerificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/rejected-documents")
public class UplodeRejectedDocumentsController {

    @Autowired
    private UplodeRejectedDocumentsService uplodeRejectedDocumentsService;


    @GetMapping("/screening/get-rejected-doc/{applicationId}")
    public ResponseEntity<ApiResponse<ScreeningAndZonalRejectedDocuments>> getRejectDocuments(@PathVariable UUID applicationId){
        ScreeningAndZonalRejectedDocuments res = uplodeRejectedDocumentsService.getRejectedDocuments(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(res,"Reject documents found"));
    }

    @PostMapping(value = "/screening/upload-rejected-doc/{verificationId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateApplicationDocumentVerificationDTO>> uploadRejectedDocuments(@PathVariable UUID verificationId, @RequestParam MultipartFile file) throws IOException {
        CandidateApplicationDocumentVerificationDTO res = uplodeRejectedDocumentsService.uploadRejectedDocuments(verificationId,file);
        return ResponseEntity.ok(ApiResponse.ok(res,"Document uploaded successfully"));
    }

    @GetMapping("/zonal/get-rejected-doc/{applicationId}")
    public ResponseEntity<ApiResponse<ScreeningAndZonalRejectedDocuments>> getRejectedDocumentsZonal(@PathVariable UUID applicationId){
        ScreeningAndZonalRejectedDocuments res = uplodeRejectedDocumentsService.getRejectedDocumentsZonal(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(res,"Reject documents found"));
    }

    @PostMapping(value = "/zonal/upload-rejected-doc/{verificationId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateApplicationDocumentVerificationDTO>> uplodeRejectedDocumentsZonal(@PathVariable UUID verificationId, @RequestParam MultipartFile file) throws IOException {
        CandidateApplicationDocumentVerificationDTO res = uplodeRejectedDocumentsService.uplodeRejectedDocumentsZonal(verificationId,file);
        return ResponseEntity.ok(ApiResponse.ok(res,"Document uploaded successfully"));
    }
}
