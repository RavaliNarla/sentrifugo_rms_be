package com.bob.candidateportal.controllers;

import com.bob.candidateportal.model.PreOnboardingPrevOrgDetailsModel;
import com.bob.candidateportal.model.PreOnboardingProfileModel;
import com.bob.candidateportal.service.PreOnboardingProfileService;
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
@RequestMapping("${candidate.api.base.path}/pre-onboarding-profile")
public class PreOnboardingProfileController {

    @Autowired
    private PreOnboardingProfileService preOnboardingProfileService;



    @GetMapping("/get-profile-details/{applicationId}")
    public ResponseEntity<ApiResponse<Object>> getPreOnboardingDetailsByAppId(@PathVariable UUID applicationId){
        Object preOnboardingDetails = preOnboardingProfileService.getPreOnboardingDetailsByAppId(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(preOnboardingDetails,"Pre-Onboarding details fetched successfully"));

    }

    @PostMapping(value = "/save-or-update-profile/{applicationId}")
    public ResponseEntity<ApiResponse<String>> saveOrUpdatePreOnboardingProfile(@PathVariable UUID applicationId, @RequestBody PreOnboardingProfileModel profile) throws IOException {
        preOnboardingProfileService.saveOrUpdatePreOnboardingProfile(applicationId, profile);
        return ResponseEntity.ok(ApiResponse.ok("Pre-Onboarding profile saved/updated successfully"));
    }

    @PostMapping(value = "/upload-profile-file/{applicationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> uploadPreOnboardingFile(@PathVariable UUID applicationId,
                                                                       @RequestParam PreOnboardingDocumentType documentType,
                                                                       @RequestParam MultipartFile file) {
        Object uploadedFileResult= preOnboardingProfileService.uploadPreOnboardingFile(applicationId, documentType, file);
        return ResponseEntity.ok(ApiResponse.ok(uploadedFileResult,"File uploaded successfully"));
    }



}
