package com.bob.jobportal.model;

import com.bob.db.dto.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterviewAllocatedRequestModel {
    private String fullName;
    private CandidateApplicationsDTO application;
    private InterviewPanelsDTO interviewPanels;
    private InterviewCentresDTO interviewCentres;
    private InterviewScheduleStagingDTO interviewScheduleStaging;
}
