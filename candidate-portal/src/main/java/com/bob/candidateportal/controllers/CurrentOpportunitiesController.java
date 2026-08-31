package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CurrentOpportunitiesService;
import com.bob.candidateportal.model.ActiveJobPositionResponseModel;
import com.bob.candidateportal.model.JobPositionFilterRequestModel;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.JobRequisitionsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${candidate.api.base.path}/current-opportunities")
public class CurrentOpportunitiesController {

    @Autowired
    private CurrentOpportunitiesService currentOpportunitiesService;


    @GetMapping("/get-job-requisition/active")
    public ResponseEntity<ApiResponse<List<JobRequisitionsDTO>>>  getActiveRequisitions() {
        List<JobRequisitionsDTO> activeJobs = currentOpportunitiesService.getActiveRequisitions();
        if (activeJobs.isEmpty()) {
            ApiResponse<List<JobRequisitionsDTO>> apiResponse = new ApiResponse<>(
                    false,
                    "No active jobs found",
                    activeJobs
            );
            return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
        }

        ApiResponse<List<JobRequisitionsDTO>> apiResponse = new ApiResponse<>(
                true,
                AppConstants.ACTIVE_JOBS_FOUND_MESSAGE,
                activeJobs
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/get-job-positions/active")
    public ResponseEntity<ApiResponse<Page<ActiveJobPositionResponseModel>>> getActiveJobPositions(@RequestBody JobPositionFilterRequestModel model) {
        Page<ActiveJobPositionResponseModel> activeJobs = currentOpportunitiesService.getActiveJobPositions(model);

        ApiResponse<Page<ActiveJobPositionResponseModel>> apiResponse = new ApiResponse<>(
                true,
                AppConstants.ACTIVE_JOBS_FOUND_MESSAGE,
                activeJobs
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
    @PostMapping("/public/get-job-positions/active")
    public ResponseEntity<ApiResponse<Page<ActiveJobPositionResponseModel>>> getPublicActiveJobPositions(@RequestBody JobPositionFilterRequestModel model) {
        Page<ActiveJobPositionResponseModel> activeJobs = currentOpportunitiesService.getActiveJobPositions(model);


        ApiResponse<Page<ActiveJobPositionResponseModel>> apiResponse = new ApiResponse<>(
                true,
                AppConstants.ACTIVE_JOBS_FOUND_MESSAGE,
                activeJobs
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
