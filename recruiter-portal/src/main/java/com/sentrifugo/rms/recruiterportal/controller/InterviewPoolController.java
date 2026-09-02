package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.recruiterportal.dto.InterviewScheduleDTO;
import com.sentrifugo.rms.recruiterportal.dto.ScoreSubmitRequest;
import com.sentrifugo.rms.recruiterportal.service.InterviewPoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Interview Pool")
@RestController
@RequestMapping("${recruiter.api.base.path}/interview-pool")
@RequiredArgsConstructor
public class InterviewPoolController {

    private final InterviewPoolService interviewPoolService;

    @Operation(summary = "List interview pool candidates for a position (server-side paginated)")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<InterviewScheduleDTO>>> search(
            @RequestParam UUID positionId,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false, defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<CandidateStatus> statusList = statuses == null || statuses.isEmpty() ? null :
                statuses.stream().map(CandidateStatus::valueOf).toList();
        return ResponseEntity.ok(ApiResponse.ok(
                interviewPoolService.getInterviewPool(positionId, statusList, searchText, page, size),
                "Interview pool fetched successfully"));
    }

    @Operation(summary = "List candidates scheduled with the current interviewer's panel(s) for a position")
    @GetMapping("/my-interviews")
    public ResponseEntity<ApiResponse<List<InterviewScheduleDTO>>> myInterviews(@RequestParam UUID positionId) {
        return ResponseEntity.ok(ApiResponse.ok(interviewPoolService.getMyInterviews(positionId), "Your interviews fetched successfully"));
    }

    @Operation(summary = "Submit a score/comment for a candidate as the currently logged-in panel member")
    @PostMapping("/score")
    public ResponseEntity<ApiResponse<Void>> submitScore(@Valid @RequestBody ScoreSubmitRequest request) {
        interviewPoolService.submitScore(request);
        return ResponseEntity.ok(ApiResponse.ok("Score submitted successfully"));
    }

    @Operation(summary = "Submit scores for multiple candidates in one batch")
    @PostMapping("/score-batch")
    public ResponseEntity<ApiResponse<Void>> submitScoreBatch(@Valid @RequestBody List<ScoreSubmitRequest> requests) {
        requests.forEach(interviewPoolService::submitScore);
        return ResponseEntity.ok(ApiResponse.ok("Scores submitted successfully"));
    }
}
