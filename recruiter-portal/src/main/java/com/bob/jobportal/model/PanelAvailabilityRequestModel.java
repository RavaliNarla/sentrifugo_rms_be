package com.bob.jobportal.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;
@Data
public class PanelAvailabilityRequestModel {
    private UUID panelId;
    private LocalDate panelStartDate;
    private LocalDate panelEndDate;
}
