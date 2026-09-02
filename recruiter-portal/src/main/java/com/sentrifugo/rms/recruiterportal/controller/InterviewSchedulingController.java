package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.ScheduleInterviewRequest;
import com.sentrifugo.rms.recruiterportal.service.InterviewSchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Interview Scheduling")
@RestController
@RequestMapping("${recruiter.api.base.path}/interview-scheduling")
@RequiredArgsConstructor
public class InterviewSchedulingController {

    private final InterviewSchedulingService interviewSchedulingService;

    @Operation(summary = "Apply to All: generate interview slots across the given panel windows and assign one candidate per slot")
    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<Integer>> schedule(@Valid @RequestBody ScheduleInterviewRequest request) {
        int count = interviewSchedulingService.scheduleInterviews(request).size();
        return ResponseEntity.ok(ApiResponse.ok(count, "Scheduled " + count + " candidate(s) for interview"));
    }
}
