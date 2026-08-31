package com.bob.jobportal.model;

import com.bob.db.enums.RequisitionEditStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RequisitionEditDraftResponseModel {
    private UUID draftId;
    private UUID parentRequisitionId;
    private Integer baseVersionNo;
    private RequisitionEditStatus requisitionStatus;
    private String requisitionTitle;
    private String requisitionDescription;
    private LocalDate startDate;
    private LocalDate endDate;
    private String requisitionComments;
    private String indentPath;
    private LocalDate cutoffDate;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;
    private UUID publishedBy;
    private List<RequisitionEditPositionResponseModel> positions;
}
