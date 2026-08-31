package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.jobportal.service.OnboardingService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/onboarding")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'ZONAL_HR')")
public class OnboardingController {

    @Autowired
    private OnboardingService onboardingService;

    /**
     * Onboards a candidate.
     *
     * Validates that:
     *  - The application belongs to the candidate and is in PRE_ONBOARDING_COMPLETED status.
     *  - The pre-onboarding record is in SUBMITTED status.
     *
     * Then deducts remaining vacancy counts across all relevant tables and sets
     * the application status to ONBOARDED.
     */
    @PostMapping("/onboard")
    public ResponseEntity<ApiResponse<Void>> onboardCandidate(
            @RequestParam @NotNull UUID applicationId,
            @RequestParam @NotNull UUID candidateId
    ) {
        onboardingService.onboardCandidate(applicationId, candidateId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Candidate onboarded successfully"));
    }
}
