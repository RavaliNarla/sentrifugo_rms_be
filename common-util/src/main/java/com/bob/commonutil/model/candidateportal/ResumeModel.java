package com.bob.commonutil.model.candidateportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeModel {
    private ResumePersonalInformationModel personal;
    private List<ResumeExperienceInformationModel> experience;
    private List<ResumeEducationInformationModel> education;
    private List<ResumeCertificationInformationModel> certifications;
}
