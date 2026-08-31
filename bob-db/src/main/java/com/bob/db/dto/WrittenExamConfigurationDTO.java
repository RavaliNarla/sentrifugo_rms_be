package com.bob.db.dto;

import com.bob.db.enums.WrittenExamConfigurationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WrittenExamConfigurationDTO extends BaseDTO implements Serializable {
    @JsonProperty("examConfigId")
    private UUID id;
    private UUID positionId;
    private String examName;
    private Integer totalMarks;
    private Integer numberOfSections;
    private Integer marksPerSection;
    private BigDecimal writtenExamWeightage;
    private BigDecimal interviewWeightage;
    private WrittenExamConfigurationStatus status;
    private String comments;
    private List<ExamSectionPassMarksDTO> sections;
    private String positionName;
    private Boolean isFrozen = false;
}
