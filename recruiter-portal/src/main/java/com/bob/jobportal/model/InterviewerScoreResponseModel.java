package com.bob.jobportal.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response model after submitting interview scores")
public class InterviewerScoreResponseModel {

    @Schema(description = "Message indicating submission status")
    private String message;

    @Schema(description = "Whether all panel members have scored")
    private Boolean allPanelMembersScored;

    @Schema(description = "Final average score if all panel members scored")
    private BigDecimal finalScore;

    @Schema(description = "Whether the candidate is qualified (score > 50)")
    private Boolean isQualified;
}
