package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ExamSectionPassMarksDTO extends BaseDTO implements Serializable {
    @JsonProperty("examSectionId")
    private UUID id;
    private UUID examConfigId;
    private Integer sectionNumber;
    private String sectionName;
    private Boolean isRankingEnabled;
    private Integer sectionTotalMarks;
    private Boolean isStatewise;
    private List<ExamSectionCategoryPassMarksDTO> categoryPassMarks;
}
