package com.bob.jobportal.model;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class InterviewSchedulingRequestModel {
    @NotNull(message = "Scheduling panel data is required")
    private SchedulingPanelModel schedulingPanelModel;
    @NotNull(message = "Panel schedule data is required")
    private List<SchedulePanelExcelModel> panelScheduleModelList;

    private Map<UUID, UUID> zonalChangeMap;
}
