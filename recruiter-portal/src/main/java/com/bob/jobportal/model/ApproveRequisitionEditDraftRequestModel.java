package com.bob.jobportal.model;

import com.bob.db.enums.RequisitionEditStatus;
import lombok.Data;

@Data
public class ApproveRequisitionEditDraftRequestModel {
    private RequisitionEditStatus postingStatus;
    private String comments;
}
