package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class ExamSectionCategoryPassMarksDTO extends BaseDTO implements Serializable {
    @JsonProperty("examSectionCategoryId")
    private UUID id;
    private UUID examSectionId;
    private UUID categoryId;
    private Integer passMark;
    private UUID stateId;
}
