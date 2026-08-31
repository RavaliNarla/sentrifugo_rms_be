package com.bob.jobportal.model;

import com.bob.db.dto.CandidateApplicationsDTO;
import com.bob.db.dto.CandidatesDTO;
import com.bob.db.dto.InterviewCentresDTO;
import com.bob.db.dto.InterviewScheduleDTO;
import com.bob.db.dto.ReservationCategoriesDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewerCandidateResponseModel {
    
    private InterviewScheduleDTO interviewSchedule;
    
    private CandidatesDTO candidate;
    
    private CandidateApplicationsDTO application;
    
    private ReservationCategoriesDTO category;
    
    private InterviewCentresDTO interviewCentre;
    
    private BigDecimal panelScore;
    
    private String panelComments;
    
    private String resumeUrl;
    
    private String candidateFullName;  // Combined full name from first_name, middle_name, last_name in candidate_profile
}
