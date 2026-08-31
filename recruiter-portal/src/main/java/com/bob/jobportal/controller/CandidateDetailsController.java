package com.bob.jobportal.controller;

import com.bob.commonutil.model.CandidateParentModel;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.db.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/candidate-details")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class CandidateDetailsController {

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @GetMapping("/get-all-details/{candidateId}/{positionId}")
    public ResponseEntity<ApiResponse<?>> getCandidateAllDetails(@PathVariable UUID candidateId,@PathVariable UUID positionId){

        CandidateParentModel res = candidateCommonGetService.getCandidateBasicDetails(candidateId,positionId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate all details fetched successfully"));
    }
}
