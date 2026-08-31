package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CandidateWrittenExamMarksDTO extends BaseDTO implements Serializable {
    @JsonProperty("examMarkId")
    private UUID id;
    private UUID candidateId;
    private UUID applicationId;
    private UUID positionId;
    private UUID examConfigId;
    private String rollNumber;
    private BigDecimal totalMarksObtained;
    private BigDecimal rankingMarksObtained;
    private BigDecimal normalizedScore;
    private BigDecimal finalWeightedScore;
    private Integer rankNumber;
    private Boolean isPassed;
    private String status;
    private String remarks;
}
