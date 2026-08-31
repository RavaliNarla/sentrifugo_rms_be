package com.bob.candidateportal.controllers;

import com.bob.candidateportal.model.ExamCenterModel;
import com.bob.candidateportal.service.AppliedJobsService;
import com.bob.candidateportal.model.AppliedJobResponseModel;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/applied-jobs")
public class AppliedJobsController {

    @Autowired
    private AppliedJobsService appliedJobsService;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping("get-applied-jobs")
    public ResponseEntity<ApiResponse<Page<AppliedJobResponseModel>>> getAppliedJobPositions(
                                                    @RequestParam(required = false) String searchTerm,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size){
        UUID candidateId = securityUtils.getCurrentUserId();

        Page<AppliedJobResponseModel> appliedJobs = appliedJobsService.getAppliedJobsWithSearch(
                candidateId,
                searchTerm,
                page,
                size
        );


        ApiResponse<Page<AppliedJobResponseModel>> apiResponse = new ApiResponse<>(
                true,
                "Applied jobs found for the candidate",
                appliedJobs
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/download/application-form/{applicationId}")
    public ResponseEntity<byte[]> downloadApplication(@PathVariable UUID applicationId){
        UUID candidateId = securityUtils.getCurrentUserId();
        byte[] pdf=appliedJobsService.generateApplication(candidateId,applicationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, AppConstants.CONTENT_DISPOSITION_ATTACHMENT+"; filename=Application_Form.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/save/exam-center")
    public ResponseEntity<ApiResponse<ExamCenterModel>> saveExamCenter(@RequestBody ExamCenterModel model){
         UUID candidateId = securityUtils.getCurrentUserId();
         ExamCenterModel res = appliedJobsService.saveExamCenter(candidateId,model);
         return ResponseEntity.ok(ApiResponse.ok(res,"Exam center saved successfully"));
    }

    @GetMapping("/get/exam-center")
    public ResponseEntity<ApiResponse<ExamCenterModel>> getExamCenter(@RequestParam UUID applicationId) {
        UUID candidateId = securityUtils.getCurrentUserId();
        ExamCenterModel res = appliedJobsService.getExamCenter(candidateId, applicationId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Exam center details fetched successfully"));
    }

}
