package com.bob.jobportal.model;

import com.bob.db.enums.ButtonActionEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ApproveRejectModel {
    @NotNull(message = "approval IDs cannot be null")
    private List<UUID> offerApprovalIds;
    @NotNull(message = "Action cannot be null")
    private ButtonActionEnum action;
    private String comments;
}
