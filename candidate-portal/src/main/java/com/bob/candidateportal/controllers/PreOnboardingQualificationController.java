package com.bob.candidateportal.controllers;

import com.bob.candidateportal.model.PreOnboardingQualificationModel;
import com.bob.candidateportal.service.PreOnboardingQualificationService;
import com.bob.db.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/pre-onboarding-qualification")
public class PreOnboardingQualificationController {

    @Autowired
    private PreOnboardingQualificationService preOnboardingQualificationService;


    @GetMapping("/get-qualification-details/{applicationId}")
    public ResponseEntity<ApiResponse<Object>> getQualDetailsByAppId(@PathVariable UUID applicationId){
        Object preOnboardingQualDetails = preOnboardingQualificationService.getQualDetailsByAppId(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(preOnboardingQualDetails,"Previous employment details fetched successfully"));
    }

    @PostMapping("/save-or-update/qualification-details/{applicationId}")
    public ResponseEntity<ApiResponse<Object>> saveOrUpdateQualDetails(@PathVariable UUID applicationId,@RequestBody PreOnboardingQualificationModel qualificationModel){
        preOnboardingQualificationService.saveOrUpdateQualDetails(applicationId,qualificationModel);
        return ResponseEntity.ok(ApiResponse.ok("Previous Employment details saved/Updated Sucessfully!"));
    }

}
