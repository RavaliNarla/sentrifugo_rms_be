package com.bob.candidateportal.model;

import com.bob.db.dto.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppliedJobResponseModel {
    private JobPositionsDTO positions;
    private CandidateApplicationsDTO candidateApplication;
    private MasterPositionsDTO masterPositions;
    private JobRequisitionsDTO requisitions;
    private CandidateLocationPreferenceDTO candidateLocationPreference;
    private Boolean isWrittenExamConfigured = false;
}
