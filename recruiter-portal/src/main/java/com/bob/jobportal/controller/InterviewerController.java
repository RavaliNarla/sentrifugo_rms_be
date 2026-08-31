package com.bob.jobportal.controller;

import com.bob.commonutil.exception.CommonException;
import com.bob.db.dto.ApiResponse;
import com.bob.jobportal.model.InterviewerCandidateResponseModel;
import com.bob.jobportal.model.InterviewerPositionResponseModel;
import com.bob.jobportal.model.InterviewerScoreRequestModel;
import com.bob.jobportal.model.InterviewerScoreResponseModel;
import com.bob.jobportal.service.InterviewerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/interviewer")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
@Tag(name = "Interviewer Portal", description = "APIs for interviewers to manage interviews and evaluations")
@Slf4j
public class InterviewerController {

    @Autowired
    private InterviewerService interviewerService;

    @GetMapping("/get-panel-positions")
    @Operation(
            summary = "Get positions assigned to interviewer's panel",
            description = "Retrieves all positions assigned to the logged-in interviewer's panel. " +
                    "Validates that the user is part of an 'Interview' committee."
    )
    public ResponseEntity<ApiResponse<List<InterviewerPositionResponseModel>>> getInterviewerPositions() {
        log.info("Fetching positions for interviewer");

        List<InterviewerPositionResponseModel> positions = interviewerService.getInterviewerPositions();

        return ResponseEntity.ok(
                ApiResponse.ok(positions,
                        String.format("Found %d positions assigned to your panel(s)", positions.size()))
        );
    }

    @GetMapping("/get-candidates-by-position")
    @Operation(
            summary = "Get candidates scheduled for interview on a specific date",
            description = "Retrieves all candidates scheduled for interview with the logged-in interviewer's panel " +
                    "on the given date for a specific position. Only includes candidates with VERIFIED or " +
                    "PROVISIONALLY_APPROVED zonal verification status."
    )
    public ResponseEntity<ApiResponse<List<InterviewerCandidateResponseModel>>> getCandidatesForInterview(
            @Parameter(description = "Position ID to filter candidates", required = true)
            @RequestParam UUID positionId,
            @Parameter(description = "Interview date (format: YYYY-MM-DD)", required = true, example = "2026-02-12")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Fetching candidates for interview - positionId: {}, date: {}", positionId, date);

        List<InterviewerCandidateResponseModel> candidates = interviewerService.getCandidatesForInterview(positionId, date);

        return ResponseEntity.ok(
                ApiResponse.ok(candidates,
                        String.format("Found %d candidates scheduled for interview on %s", candidates.size(), date))
        );
    }

    @PostMapping("/save-candidate-score")
    @Operation(
            summary = "Submit interview score and comments for a candidate",
            description = "Submits the panel member's score and comments for a candidate. " +
                    "After submission, verifies if all panel members have scored. " +
                    "If all members scored, calculates the average and updates final score in interview_schedule. " +
                    "If average score > 50, sets interview status to QUALIFIED."
    )
    public ResponseEntity<ApiResponse<InterviewerScoreResponseModel>> submitInterviewScore(
            @Valid @RequestBody InterviewerScoreRequestModel request) {

        log.info("Submitting interview score for candidate: {}", request.getCandidateId());

        InterviewerScoreResponseModel response = interviewerService.submitInterviewScore(request);

        return ResponseEntity.ok(
                ApiResponse.ok(response, response.getMessage())
        );
    }

    @PostMapping("/save-candidate-scores")
    @Operation(
            summary = "Submit interview scores and comments for multiple candidates in batch",
            description = "Submits the panel member's scores and comments for multiple candidates in a single request. " +
                    "For each candidate, it verifies if all panel members have scored, calculates averages, " +
                    "and updates final scores. Returns a list of results for each candidate."
    )
    public ResponseEntity<ApiResponse<List<InterviewerScoreResponseModel>>> submitBatchInterviewScores(
            @Valid @RequestBody List<InterviewerScoreRequestModel> requests) {

        log.info("Submitting batch interview scores for {} candidates", requests.size());

        List<InterviewerScoreResponseModel> responses = interviewerService.submitBatchInterviewScores(requests);

        return ResponseEntity.ok(
                ApiResponse.ok(responses, "Batch score submission completed for " + requests.size() + " candidates")
        );
    }

    @GetMapping("/download-interview-scores-template")
    @Operation(
            summary = "Download interview scores XLSX template",
            description = "Returns an Excel file pre-populated with a Candidate dropdown (all candidates "
                    + "scheduled for the given position and date) and an Absent dropdown (True/False). "
                    + "The Comments and Score columns are left empty for the recruiter to fill. "
                    + "The hidden 'CandidateData' sheet embeds all IDs required at upload time."
    )
    public ResponseEntity<byte[]> downloadInterviewScoresTemplate(
            @Parameter(description = "Position ID", required = true)
            @RequestParam UUID positionId,
            @Parameter(description = "Interview date (YYYY-MM-DD)", required = true, example = "2026-04-07")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            byte[] fileBytes = interviewerService.generateInterviewScoresTemplate(positionId, date);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("Interview_Scores_" + date + ".xlsx")
                    .build());
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (IllegalStateException e) {
            // Propagate as-is so GlobalExceptionHandler returns 409 with the message
            throw e;
        } catch (Exception e) {
            throw new CommonException("Failed to generate interview scores template");
        }
    }

    @PostMapping(value = "/upload-interview-scores", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload filled interview scores XLSX",
            description = "Accepts the template generated by /download-interview-scores-template. "
                    + "Validates every row before saving: duplicate candidates, absent+score conflict, "
                    + "score range 0-100. If ANY row fails, nothing is saved and all errors are returned."
    )
    public ResponseEntity<ApiResponse<List<InterviewerScoreResponseModel>>> uploadInterviewScores(
            @Parameter(description = "Filled interview scores XLSX file", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Received interview scores Excel upload: {} bytes", file.getSize());
        List<InterviewerScoreResponseModel> responses = interviewerService.processInterviewScoresFromExcel(file);
        return ResponseEntity.ok(ApiResponse.ok(responses,
                "Interview scores submitted successfully for " + responses.size() + " candidate(s)."));
    }
}

