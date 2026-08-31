package com.bob.jobportal.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SchedulingPanelModel {
    @NotNull(message = "Application Ids are required")
    private List<UUID> applicationIds;
    @NotNull(message = "Position Id is required")
    private List<UUID> positionIds;
}
