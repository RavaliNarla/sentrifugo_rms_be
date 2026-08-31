package com.bob.jobportal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingConflictResponseModel {
    private String applicationNo;
    private String conflictingApplicationNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean panelConflict;
    private boolean zoneConflict;
    private boolean memberConflict;
    private String message;
}
