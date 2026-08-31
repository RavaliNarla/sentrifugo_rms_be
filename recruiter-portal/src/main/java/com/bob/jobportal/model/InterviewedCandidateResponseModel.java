package com.bob.jobportal.model;

import com.bob.db.dto.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InterviewedCandidateResponseModel {
    private String fullName;
    private CandidateApplicationsDTO application;
    private String resumeUrl;
    private InterviewScheduleDTO interviewSchedules;
    private InterviewPanelsDTO panel;
    private InterviewCentresDTO center;
    private List<InterviewPanelScheduleConfigurationDTO> panelScheduleConfiguration;
}
