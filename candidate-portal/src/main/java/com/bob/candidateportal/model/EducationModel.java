package com.bob.candidateportal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationModel {

    private String educationLevel;
    private String university;
    private String school;
    private String degree;
    private String specialization;
    private String fromDate;
    private String toDate;
    private String percentage;
}
