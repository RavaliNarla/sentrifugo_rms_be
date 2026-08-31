package com.bob.db.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CandidateExamSectionMarksDTO extends BaseDTO implements Serializable {
    private UUID id;
    private UUID candidateId;
    private UUID applicationId;
    private UUID examConfigId;
    private UUID examSectionId;
    private Integer sectionNumber;
    private BigDecimal obtainedMarks;
    private Boolean isQualified;
    private Boolean isRankingEnabled;
    private String remarks;
}
