package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CandidateSectionMarksDTO extends BaseDTO implements Serializable {
    @JsonProperty("sectionMarkId")
    private UUID id;
    private UUID candidateExamId;
    private UUID examSectionId;
    private BigDecimal marksObtained;
    private Boolean isSectionPassed;
    private Boolean isRankingEnabled;
}
