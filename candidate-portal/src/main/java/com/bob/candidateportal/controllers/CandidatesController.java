package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CandidateService;
import com.bob.commonutil.model.CandidateParentModel;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/candidate")
public class CandidatesController {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping("/get-all-details/{positionId}")
    public ResponseEntity<ApiResponse<?>> getCandidateAllDetails(@PathVariable UUID positionId){
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateParentModel res = candidateService.getCandidateBasicDetails(candidateId,positionId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate all details fetched successfully"));
    }

    @PostMapping("/save-profile-complete")
    public ResponseEntity<ApiResponse<?>> saveCandidateProfileComplete(@RequestParam Boolean isProfileCompleted){
        UUID candidateId = securityUtils.getCurrentUserId();
        Boolean res = candidateService.saveCandidateProfileComplete(candidateId,isProfileCompleted);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate profile completion status updated successfully"));
    }


}