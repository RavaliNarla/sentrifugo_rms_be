package com.bob.commonutil.model.candidateportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeExperienceInformationModel {
    private String employer;
    private String designation;
    private String startDate;
    private String endDate;
    private String location;
    private String totalYears;
}
