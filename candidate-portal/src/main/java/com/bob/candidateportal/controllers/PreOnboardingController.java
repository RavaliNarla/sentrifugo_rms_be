package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.PreOnboardingService;
import com.bob.db.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/pre-onboarding")
public class PreOnboardingController {

    @Autowired
    private PreOnboardingService preOnboardingService;

    @PostMapping("/save-onboarding/{applicationId}")
    public ResponseEntity<ApiResponse<Object>> saveOnboardingDetails(@PathVariable UUID applicationId){
        Object savedPreOnboardingDetails=preOnboardingService.savePreOnboarding(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(savedPreOnboardingDetails,"Prenboarding details saved successfully!"));
    }

    @DeleteMapping("/delete-document/{preOnboardingDocId}")
    public ResponseEntity<ApiResponse<Object>> deleteDocumentByDocId(@PathVariable UUID preOnboardingDocId){
        Object deletedDocumentDocId=preOnboardingService.deleteDocumentByDocId(preOnboardingDocId);
        return ResponseEntity.ok(ApiResponse.ok(deletedDocumentDocId,"PreOnboarding Document Deleted successfully!"));
    }
}
