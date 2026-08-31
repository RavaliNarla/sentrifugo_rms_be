package com.bob.jobportal.model;

import com.bob.db.dto.CandidateApplicationsDTO;
import com.bob.db.dto.CandidatesDTO;
import com.bob.db.dto.InterviewCentresDTO;
import com.bob.db.dto.InterviewScheduleDTO;
import com.bob.db.dto.JobRequisitionsDTO;
import com.bob.db.dto.MasterPositionsDTO;
import com.bob.db.dto.ReservationCategoriesDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZonalVerificationResponseModel {

    // Nested DTOs - directly use existing DTOs instead of duplicating fields
    private InterviewScheduleDTO interviewSchedule;
    
    private CandidatesDTO candidate;

    private CandidateApplicationsDTO application;
    
    private InterviewCentresDTO zonalOffice;
    
    private ReservationCategoriesDTO category;
    
    private MasterPositionsDTO masterPosition;
    
    private JobRequisitionsDTO requisition;
    
    // Additional fields not covered by DTOs
    private String categoryId;  // from CandidateProfileEntity - always present even if category is null
    private String resumeUrl;   // from CandidateDocumentStoreEntity - candidate's resume file URL
    private String candidateFullName;  // Combined full name from first_name, middle_name, last_name in candidate_profile
}
