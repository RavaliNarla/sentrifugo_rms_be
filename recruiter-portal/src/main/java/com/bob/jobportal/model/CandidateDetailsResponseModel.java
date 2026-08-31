package com.bob.jobportal.model;

import com.bob.db.dto.CandidateApplicationsDTO;
import com.bob.db.dto.InterviewCentresDTO;
import com.bob.db.entity.CandidateRankingResultsEntity;
import com.bob.db.enums.ExamQualificationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CandidateDetailsResponseModel {
    private String fullName;
    private Integer rank;
    private Integer totalMonths;
    private UUID stateId;
    private UUID categoryId;
    private String resumeUrl;
    private CandidateRankingResultsEntity candidateRankingResults;
    private CandidateApplicationsDTO candidateApplications;
    private InterviewCentresDTO interviewCenter;
    private BigDecimal totalMarksObtained;
    private ExamQualificationStatus examQualificationStatus;

}
