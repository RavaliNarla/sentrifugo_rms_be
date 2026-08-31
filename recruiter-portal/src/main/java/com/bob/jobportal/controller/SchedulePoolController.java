package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.InterviewScheduleDTO;
import com.bob.db.dto.InterviewScheduleStagingDTO;
import com.bob.db.enums.InterviewSchedulingApprovalStatus;
import com.bob.jobportal.model.*;
import com.bob.jobportal.service.SchedulePoolService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/schedule-pool")
public class SchedulePoolController {

    @Autowired
    private SchedulePoolService schedulePoolService;


    @PostMapping("/get-schedule-pool-candidates")
    public ResponseEntity<ApiResponse<Page<SchedulePoolModel>>> getSchedulePoolCandidates(@RequestBody SchedulePoolRequestFilterModel request) {
        Page<SchedulePoolModel> result = schedulePoolService.getSchedulePoolCandidates(request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Fetched schedule pool candidates successfully"));
    }

    @PostMapping("/get-schedule-pool-candidate-list")
    public ResponseEntity<ApiResponse<List<SchedulePoolModel>>> getSchedulePoolCandidateList(@RequestBody SchedulePoolRequestFilterModel request) {
        List<SchedulePoolModel> result = schedulePoolService.getSchedulePoolCandidatesList(request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Fetched schedule pool candidates successfully"));
    }

    @PostMapping("/submit-for-approval")
    public ResponseEntity<ApiResponse<List<InterviewScheduleStagingDTO>>> submitForApproval(@RequestBody List<UUID> positionIds) throws MessagingException, IOException {
        List<InterviewScheduleStagingDTO> result=schedulePoolService.submitForApproval(positionIds);
        return ResponseEntity.ok(ApiResponse.ok(result,"Approved Schedule pool candidates successfully"));
    }

    record SubmitL1ApprovalRequest(List<UUID> positionIds, InterviewSchedulingApprovalStatus status,String remarks){}

    @PostMapping("/submit-l1-approval")
    public ResponseEntity<ApiResponse<List<InterviewScheduleDTO>>> submitL1Approval(@RequestBody SubmitL1ApprovalRequest request) throws MessagingException, IOException {
        List<InterviewScheduleDTO> result = schedulePoolService.submitForl1Approval(request.positionIds(),request.status,request.remarks);
        return ResponseEntity.ok(ApiResponse.ok(result, "Status updated successfully."));
    }


    @GetMapping("/get-position-details-interview-approval/{requisitionId}")
    public ResponseEntity<ApiResponse<List<InterviewSchedulePoolApprovalResponse>>> getInterviewApproval(@PathVariable UUID requisitionId){
        List<InterviewSchedulePoolApprovalResponse> result=schedulePoolService.getInterviewApproval(requisitionId);
        return ResponseEntity.ok(ApiResponse.ok(result,"Fetched position  interview approval successfully"));
    }

    @GetMapping("/l1-pending-scheduled-candidates-excel/{positionId}")
    public ResponseEntity<byte[]> getInterviewScheduledCandidates(@PathVariable UUID positionId){
        byte[] excelBytes =schedulePoolService.getInterviewScheduledCandidatesExcel(positionId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=candidate_details.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

//    @PostMapping("/get-approval-history/{requisitionId}")
//    public ResponseEntity<ApiResponse<List<InterviewSchedulePoolApprovalResponse>>> getApprovalHistory(@PathVariable UUID requisitionId){
//        List<InterviewSchedulePoolApprovalResponse> result=schedulePoolService.getApprovalHistory(requisitionId);
//        return ResponseEntity.ok(ApiResponse.ok(result,"Fetched schedule pool approval history successfully"));
//     }


}
