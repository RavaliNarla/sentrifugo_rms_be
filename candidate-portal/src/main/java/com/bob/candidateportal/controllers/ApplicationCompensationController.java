package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.ApplicationCompensationService;
import com.bob.candidateportal.model.ApplicationCompensationModel;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.ApplicationCompensationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/application-compensation")
public class ApplicationCompensationController {

    @Autowired
    private ApplicationCompensationService applicationCompensationService;

    @PostMapping(value = "/save-compensation-details")
    public ResponseEntity<ApiResponse<?>> saveCompensationDetails(@RequestBody ApplicationCompensationModel requestModel) throws Exception {
        applicationCompensationService.saveCompensationDetails(requestModel);
        return ResponseEntity.ok(ApiResponse.ok("Compensation details saved successfully"));
    }

    @GetMapping("/get-compensation-details/{applicationId}")
    public ResponseEntity<ApiResponse<?>> getCompensationDetails(@PathVariable("applicationId") UUID applicationId) {
        ApplicationCompensationModel res = applicationCompensationService.getCompensationDetails(applicationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Compensation details fetched successfully", res));
    }
}
