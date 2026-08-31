package com.bob.jobportal.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SchedulePanelModel {
    private List<InterviewAllocatedRequestModel> allocatedRequestModelList;
    private List<SchedulePanelExcelModel> panelExcelModelList;
}
