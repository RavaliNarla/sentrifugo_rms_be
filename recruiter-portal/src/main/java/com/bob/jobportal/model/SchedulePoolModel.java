package com.bob.jobportal.model;

import com.bob.db.dto.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SchedulePoolModel {
    private String fullName;
    private CandidateApplicationsDTO application;
    private InterviewPanelsDTO interviewPanels;
    private InterviewCentresDTO interviewCentres;
    private InterviewScheduleStagingDTO interviewScheduleStaging;
    private String resumeUrl;
    private List<InterviewPanelScheduleConfigurationDTO> panelScheduleConfigurations;
}
