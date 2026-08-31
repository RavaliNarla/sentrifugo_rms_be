package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CandidateJobAppService;
import com.bob.candidateportal.service.JobEligibilityValidationService;
import com.bob.candidateportal.model.JobEligibilityValidationRequest;
import com.bob.candidateportal.model.UpdateOfferDecisionModel;
import com.bob.commonutil.model.SmsRequest;
import com.bob.commonutil.service.SmsService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CandidateApplicationsDTO;
import com.bob.db.dto.CandidateLocationPreferenceDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/applications")
public class CandidateJobAppController {

    @Autowired
    private CandidateJobAppService appService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private JobEligibilityValidationService eligibilityValidationService;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping("/apply/job")
    public ResponseEntity<ApiResponse<CandidateApplicationsDTO>> applyJob(@Valid @RequestBody CandidateLocationPreferenceDTO candidateLocationPreferenceDTO){
        CandidateApplicationsDTO application =appService.applyForJob(candidateLocationPreferenceDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(application, AppConstants.CAND_APP_STATUS_APPLIED_FOR_JOB));
    }

    @PostMapping("/update-offer-decision")
    public ResponseEntity<ApiResponse<?>> updateOfferDecision(@RequestBody UpdateOfferDecisionModel model){
        return ResponseEntity.ok(ApiResponse.ok(appService.updateOfferDecision(model),"Offer Letter Updated successfully!"));
    }

    @PostMapping("/validate-eligibility")
    public ResponseEntity<ApiResponse<Object>> validateEligibility(
            @Valid @RequestBody JobEligibilityValidationRequest request) {
        
        Object validationResult =
                eligibilityValidationService.validateEligibility(request.getCandidateId(), request.getPositionId());

        // If validation result is a String, it means there's a requisition conflict
        if (validationResult instanceof String) {
            return ResponseEntity.ok(
                    ApiResponse.error((String) validationResult)
            );
        }

        // Otherwise, it's a successful validation response
        return ResponseEntity.ok(
                ApiResponse.ok(validationResult, "Eligibility validation completed successfully")
        );
    }

    @GetMapping("/check-state-eligibility")
    public ResponseEntity<ApiResponse<Boolean>> checkStateEligibility(
            @RequestParam UUID positionId,
            @RequestParam UUID stateId,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID languageId) {

        UUID candidateId = securityUtils.getCurrentUserId();
        ApiResponse<Boolean> result = eligibilityValidationService.checkStateEligibility(candidateId, positionId, stateId, cityId, languageId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test-sms")
    public ResponseEntity<ApiResponse<SmsService.SmsResponse>> testSmsService(@RequestBody SmsRequest smsRequest) {
        try {
            SmsService.SmsResponse response = smsService.sendSms(smsRequest);
            return ResponseEntity.ok(ApiResponse.ok(response, "SMS sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send SMS: " + e.getMessage()));
        }
    }
}
