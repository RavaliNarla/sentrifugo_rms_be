package com.bob.jobportal.model;

import com.bob.db.dto.JobPositionsDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class InterviewSchedulePoolApprovalResponse {
    private JobPositionsDTO jobPosition;
    private InterviewApprovalResponseModel interviewApprovalDetails;
    private List<InterviewApprovalResponseModel> interviewApprovalHistory;
}
