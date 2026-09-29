package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.InterviewScheduleListItemDTO;
import com.sentrifugo.rms.recruiterportal.dto.ScheduleInterviewRequest;
import com.sentrifugo.rms.recruiterportal.service.InterviewSchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Interview Scheduling")
@RestController
@RequestMapping("${recruiter.api.base.path}/interview-scheduling")
@RequiredArgsConstructor
public class InterviewSchedulingController {

    private final InterviewSchedulingService interviewSchedulingService;

    @Operation(summary = "Schedule shortlisted candidates against one panel for a single day")
    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<Integer>> schedule(@Valid @RequestBody ScheduleInterviewRequest request) {
        int count = interviewSchedulingService.scheduleInterviews(request).size();
        return ResponseEntity.ok(ApiResponse.ok(count, "Scheduled " + count + " candidate(s) for interview"));
    }

    @Operation(summary = "Reschedule INVITE_SENT / SCHEDULED interviews (blocked if any scores exist)")
    @PostMapping("/reschedule")
    public ResponseEntity<ApiResponse<Integer>> reschedule(@Valid @RequestBody ScheduleInterviewRequest request) {
        int count = interviewSchedulingService.rescheduleInterviews(request).size();
        return ResponseEntity.ok(ApiResponse.ok(count, "Rescheduled " + count + " candidate(s)"));
    }

    @Operation(summary = "Cancel INVITE_SENT / SCHEDULED interviews and free panel slots")
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Integer>> cancel(@RequestBody Map<String, List<UUID>> body) {
        List<UUID> ids = body != null ? body.get("candidateIds") : null;
        int count = interviewSchedulingService.cancelInterviews(ids);
        return ResponseEntity.ok(ApiResponse.ok(count, "Cancelled " + count + " interview(s)"));
    }

    @Operation(summary = "Browse upcoming interview schedules by panel or interviewer (paginated)")
    @GetMapping("/schedules")
    public ResponseEntity<ApiResponse<Page<InterviewScheduleListItemDTO>>> listSchedules(
            @RequestParam String view,
            @RequestParam UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                interviewSchedulingService.listSchedules(view, id, from, to, page, size),
                "Interview schedules fetched successfully"));
    }
}
