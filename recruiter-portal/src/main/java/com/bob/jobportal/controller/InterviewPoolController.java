package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.jobportal.model.InterviewedCandidateRequestModel;
import com.bob.jobportal.model.InterviewedCandidateResponseModel;
import com.bob.jobportal.model.PanelMemberScoreResponseModel;
import com.bob.jobportal.service.InterviewPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/interview-pool")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class InterviewPoolController {

    @Autowired
    private InterviewPoolService interviewPoolService;

    @PostMapping("/get/interviewed-candidates")
    public ResponseEntity<ApiResponse<?>> getInterviewedCandidates(@RequestBody InterviewedCandidateRequestModel requestModel){
        Page<InterviewedCandidateResponseModel> responseModelPage=interviewPoolService.getInterviewedCandidates(requestModel);
        return ResponseEntity.ok(ApiResponse.ok(responseModelPage,"Interviewed candidates fetched successfully"));
    }

    @GetMapping("/get-panel-scores/{scheduledInterviewId}")
    public ResponseEntity<ApiResponse<?>> getIndividualPanelScores(@PathVariable UUID scheduledInterviewId){
        List<PanelMemberScoreResponseModel> responseModel=interviewPoolService.getIndividualPanelScores(scheduledInterviewId);
        return ResponseEntity.ok(ApiResponse.ok(responseModel,"Individual panel scores fetched successfully"));
     }
}
