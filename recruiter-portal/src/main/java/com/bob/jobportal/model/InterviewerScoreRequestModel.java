package com.bob.jobportal.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request model for submitting interview scores and comments")
public class InterviewerScoreRequestModel {

    @NotNull(message = "Application ID is required")
    @Schema(description = "Application ID from candidate applications", required = true)
    private UUID applicationId;

    @NotNull(message = "Scheduled interview ID is required")
    @Schema(description = "Scheduled interview ID from interview_schedule table", required = true)
    private UUID scheduledInterviewId;

    @NotNull(message = "Candidate ID is required")
    @Schema(description = "Candidate ID", required = true)
    private UUID candidateId;

    @NotNull(message = "Panel ID is required")
    @Schema(description = "Panel ID from interview_panels", required = true)
    private UUID panelId;

    @DecimalMin(value = "0.0", message = "Panel score must be at least 0")
    @DecimalMax(value = "100.0", message = "Panel score cannot exceed 100")
    @Schema(description = "Panel member's score for the candidate (0-100)", example = "75.50")
    private BigDecimal panelScore;

    @Schema(description = "Panel member's comments for the candidate")
    private String panelComments;

    @Schema(description = "Interview center ID")
    private UUID interviewCenterId;

    @NotNull(message = "Absent status is required")
    @Schema(description = "Whether candidate was absent", required = true)
    private Boolean isAbsent;
}
