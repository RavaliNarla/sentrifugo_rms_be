package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CandidateCompensationDTO;
import com.bob.jobportal.model.CandidateCompensationActionRequestModel;
import com.bob.jobportal.model.CompensationCandidateRequestModel;
import com.bob.jobportal.model.CompensationCandidateResponseModel;
import com.bob.jobportal.model.CompensationSendRequestModel;
import com.bob.jobportal.service.CandidateCompensationService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("${recruiter.api.base.path}/candidate-compensation")
public class CandidateCompensationController {

    @Autowired
    private CandidateCompensationService compensationService;

    @PostMapping("/send-to-compensation-pool")
    public ResponseEntity<ApiResponse<?>> sendToCompensationPool(@RequestBody CompensationSendRequestModel requestModel) throws MessagingException, IOException {
        compensationService.sendToCompensationPool(requestModel);
        return ResponseEntity.ok(ApiResponse.ok("Applications sent to compensation pool successfully"));
    }

    @PostMapping("/get-compensation-candidates")
    public ResponseEntity<ApiResponse<?>> getCompensationCandidates(@RequestBody @Valid CompensationCandidateRequestModel requestModel) {
        Page<CompensationCandidateResponseModel> candidateResponseModelPage= compensationService.getCompensationCandidates(requestModel);
        return ResponseEntity.ok(ApiResponse.ok(candidateResponseModelPage,"Compensation candidates fetched successfully"));
    }


    @PostMapping("/add-compensation-details")
    public ResponseEntity<ApiResponse<?>> addCompensationDetails(@RequestBody CandidateCompensationActionRequestModel requestModel) throws Exception {
        compensationService.addCompensationDetails(requestModel);
        return ResponseEntity.ok(ApiResponse.ok("Compensation details added successfully"));

    }
}
