package com.bob.candidateportal.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkExperienceModel {
    private String organization;
    private String post;
    private String role;
    private String fromDate;
    private String toDate;
    private String duration;
    private String description;
}
