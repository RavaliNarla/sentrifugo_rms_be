package com.bob.jobportal.model;

import com.bob.db.dto.JobPositionsDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class PanelAvailabilityModel {
    private LocalDate panelDate;
    private List<PositionPanelAvailableModel> panelAvailableModels;

    @Data
    @Builder
    public static class PositionPanelAvailableModel{
        private String positionName;
        private LocalTime startTime;
        private LocalTime endTime;
    }
}
