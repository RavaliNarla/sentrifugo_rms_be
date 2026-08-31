package com.bob.jobportal.controller;

import com.azure.core.annotation.Get;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CandidateApplicationsDTO;
import com.bob.db.dto.CandidateScreeningDTO;
import com.bob.db.entity.CandidateApplicationsEntity;
import com.bob.jobportal.model.CandidateDetailsFilterRequestModel;
import com.bob.jobportal.model.CandidateDetailsResponseModel;
import com.bob.jobportal.service.CandidateScreeningService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/candidate-screening")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class CandidateScreeningController {

    @Autowired
    private CandidateScreeningService candidateScreeningService;

    @PostMapping("/get-candidate-details")
    public ResponseEntity<ApiResponse<Page<CandidateDetailsResponseModel>>> getCandidateDetails(@RequestBody CandidateDetailsFilterRequestModel model){
        Page<CandidateDetailsResponseModel> res = candidateScreeningService.getCandidateDetails(model);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate details fetched successfully"));
    }

    @PostMapping("/save-candidate-discrepancy-details")
    public ResponseEntity<ApiResponse<CandidateScreeningDTO>> saveCandidateDiscrepancyDetails(@Valid @RequestBody CandidateScreeningDTO model){
        CandidateScreeningDTO res = candidateScreeningService.saveCandidateDiscrepancyDetails(model);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate discrepancy details saved successfully"));
    }

    @GetMapping("/get-candidate-discrepancy-details")
    public ResponseEntity<ApiResponse<CandidateScreeningDTO>> getCandidateDiscrepancyDetails(@RequestParam UUID applicationId){
        CandidateScreeningDTO res = candidateScreeningService.getCandidateDiscrepancyDetails(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate discrepancy details fetched successfully"));
    }

}
