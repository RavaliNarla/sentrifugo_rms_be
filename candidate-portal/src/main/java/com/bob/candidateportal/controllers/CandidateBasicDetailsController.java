package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CandidateBasicDetailsService;
import com.bob.commonutil.model.BasicDetailsModel;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/profile")
public class CandidateBasicDetailsController {

    @Autowired
    private CandidateBasicDetailsService candidateBasicDetailsService;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping("/save-profile-details")
    public ResponseEntity<ApiResponse<?>> updateProfile(@Valid @RequestBody BasicDetailsModel basicDetails) {
        UUID candidateId = securityUtils.getCurrentUserId();
        BasicDetailsModel res = candidateBasicDetailsService.createOrUpdateBasicDetails(candidateId, basicDetails);
        return ResponseEntity.ok(ApiResponse.ok(res,"Profile updated successfully"));
    }

    @GetMapping("/get-details")
    public ResponseEntity<ApiResponse<?>> getCandidateProfileById() {
        UUID candidateId = securityUtils.getCurrentUserId();
        BasicDetailsModel profileDTO = candidateBasicDetailsService.getCandidateProfileById(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(profileDTO, "Candidate profile fetched successfully"));
    }

    @PostMapping("/save-work-status")
    public ResponseEntity<ApiResponse<?>> saveFresherStatus( @RequestParam Boolean isFresher) {
        UUID candidateId = securityUtils.getCurrentUserId();
        candidateBasicDetailsService.saveFresherStatus(candidateId, isFresher);
        return ResponseEntity.ok(ApiResponse.ok(null, "Fresher status updated successfully"));
    }

    @GetMapping("/get-work-status")
    public ResponseEntity<ApiResponse<?>> getFresherStatus() {
        UUID candidateId = securityUtils.getCurrentUserId();
        Boolean isFresher = candidateBasicDetailsService.getFresherStatus(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(isFresher, "Fresher status fetched successfully"));
    }

    @PostMapping("/save-personal-disclaimer")
    public ResponseEntity<ApiResponse<?>> savePersonalDisclaimer(@RequestParam Boolean personalDisclaimer) {
        UUID candidateId = securityUtils.getCurrentUserId();
        candidateBasicDetailsService.savePersonalDisclaimer( candidateId,personalDisclaimer);
        return ResponseEntity.ok(ApiResponse.ok(null, "Personal disclaimer saved successfully"));
    }

    @GetMapping("/get-personal-disclaimer")
    public ResponseEntity<ApiResponse<?>> getPersonalDisclaimer() {
        UUID candidateId = securityUtils.getCurrentUserId();
        Boolean personalDisclaimer = candidateBasicDetailsService.getPersonalDisclaimer(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(personalDisclaimer, "Personal disclaimer fetched successfully"));
    }

}
