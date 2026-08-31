package com.bob.candidateportal.controllers;


import com.bob.candidateportal.service.CandidateAddressService;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CandidateAddressDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/address")
public class CandidateAddressController {

    @Autowired
    private CandidateAddressService candidateAddressService;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping("save-address")
    public ResponseEntity<ApiResponse<?>> saveCandidateAddress(@Valid @RequestBody CandidateAddressDTO candidateAddress) {
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateAddressDTO res = candidateAddressService.saveCandidateAddress(candidateId,  candidateAddress);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate address saved successfully"));
    }

    @GetMapping("/get-address")
    public ResponseEntity<ApiResponse<?>> getCandidateAddress() {
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateAddressDTO res = candidateAddressService.getCandidateAddress(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate address fetched successfully"));
    }

}
