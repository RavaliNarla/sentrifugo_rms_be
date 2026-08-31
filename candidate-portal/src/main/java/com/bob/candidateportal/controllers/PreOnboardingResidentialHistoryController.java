package com.bob.candidateportal.controllers;

import com.bob.candidateportal.model.PreOnboardingPrevOrgDetailsModel;
import com.bob.candidateportal.service.PreOnboardingResidentialHistoryService;
import com.bob.db.dto.ApiResponse;
import com.bob.db.enums.PreOnboardingDocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
@RestController
@RequestMapping("${candidate.api.base.path}/pre-onboarding-resident-history")
public class PreOnboardingResidentialHistoryController {

    @Autowired
    private PreOnboardingResidentialHistoryService residentialHistoryService;


    @GetMapping("/get-previous-employment-details/{applicationId}")
    public ResponseEntity<ApiResponse<Object>> getPreviousEmploymentDetails(@PathVariable UUID applicationId){
        Object previousEmploymentDetails = residentialHistoryService.getPreviousEmploymentDetails(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(previousEmploymentDetails,"Previous employment details fetched successfully"));
    }

    @PostMapping(value = "/save-or-update-resident-details/{applicationId}")
    public ResponseEntity<ApiResponse<String>> saveOrUpdatePrevOrgDetails(@PathVariable UUID applicationId, @RequestBody PreOnboardingPrevOrgDetailsModel prevOrgDetailsModel) throws IOException {
        residentialHistoryService.saveOrUpdatePrevOrgDetails(applicationId, prevOrgDetailsModel);
        return ResponseEntity.ok(ApiResponse.ok("Pre-Onboarding profile saved/updated successfully"));
    }

    @PostMapping(value = "/upload-onboarding-file/{applicationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> uploadPreOnboardingFile(@PathVariable UUID applicationId,
                                                                       @RequestParam PreOnboardingDocumentType documentType,
                                                                       @RequestParam MultipartFile file) {
        Object uploadedFileResult= residentialHistoryService.uploadPreOnboardingFile(applicationId, documentType, file);
        return ResponseEntity.ok(ApiResponse.ok(uploadedFileResult,"File uploaded successfully"));
    }
}
