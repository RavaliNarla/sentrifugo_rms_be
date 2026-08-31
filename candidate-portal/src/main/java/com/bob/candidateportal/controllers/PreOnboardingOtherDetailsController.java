package com.bob.candidateportal.controllers;

import com.bob.candidateportal.model.PreOnboardingOtherDetailsModel;
import com.bob.candidateportal.service.PreOnboardingOtherDetailsService;
import com.bob.db.dto.ApiResponse;
import com.bob.db.enums.PreOnboardingDocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/pre-onboarding-other-details")
public class PreOnboardingOtherDetailsController {
    @Autowired
    private PreOnboardingOtherDetailsService preOnboardingOtherDetailsService;

    @GetMapping("/get-other-details/{applicationId}")
    public ResponseEntity<ApiResponse<Object>> getOtherDetailsByAppId(@PathVariable UUID applicationId){
        Object preOnboardingQualDetails = preOnboardingOtherDetailsService.getOtherDetailsByAppId(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(preOnboardingQualDetails,"PreOnboarding details fetched successfully"));
    }

    @PostMapping("/save-or-update/other-details/{applicationId}")
    public ResponseEntity<ApiResponse<Object>> saveOrUpdateOtherDetails(@PathVariable UUID applicationId, @RequestBody PreOnboardingOtherDetailsModel otherDetailsModel){
        preOnboardingOtherDetailsService.saveOrUpdateOtherDetails(applicationId,otherDetailsModel);
        return ResponseEntity.ok(ApiResponse.ok("PreOnboarding Details saved/Updated Successfully!"));
    }
    @PostMapping("/upload-other-files/{applicationId}")
    public ResponseEntity<ApiResponse<Object>> uploadOtherFiles(@PathVariable UUID applicationId,
                                                                @RequestParam PreOnboardingDocumentType documentType,
                                                                @RequestParam MultipartFile file){
        Object uploadedFileResult= preOnboardingOtherDetailsService.uploadPreOnboardingFile(applicationId, documentType, file);
        return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully"));
    }
}
