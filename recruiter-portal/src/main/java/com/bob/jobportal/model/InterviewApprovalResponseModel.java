package com.bob.jobportal.model;

import com.bob.db.dto.JobPositionsDTO;
import com.bob.db.dto.PositionPanelDTO;
import com.bob.db.dto.UserDTO;
import com.bob.db.entity.PositionPanelEntity;
import com.bob.db.enums.InterviewSchedulingApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewApprovalResponseModel {
    private InterviewSchedulingApprovalStatus status;
    private LocalDateTime approvalOn;
    private Boolean isHistory;
    private Integer totalCandidateCount;
    private Integer totalZonalCount;
    private Integer totalPanelCount;

    private List<ZonalData> zonalData;
    private List<PanelData> panelData;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ZonalData {
        private UUID zonalId;
        private String zoneName;
        private Integer candidateCount;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PanelData {
        private UUID panelId;
        private String panelName;
        private List<UserDTO> members;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}