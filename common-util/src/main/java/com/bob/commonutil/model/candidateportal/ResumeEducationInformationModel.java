package com.bob.commonutil.model.candidateportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeEducationInformationModel {
    private String level;
    private String board;
    private String school;
    private String passingYear;
    private String percentage;
    private String state;
}
