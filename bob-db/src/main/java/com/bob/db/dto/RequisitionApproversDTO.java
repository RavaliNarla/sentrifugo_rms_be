package com.bob.db.dto;

import com.bob.db.entity.RequisitionApproversEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class RequisitionApproversDTO extends BaseDTO implements Serializable {

    @JsonProperty("approverId")
    private UUID id;

    @NotNull(message = "Approver ID is required")
    private UUID approverId;

    @NotNull(message = "Approver role is required")
    private RequisitionApproversEntity.ApproverRole approverRole;
}
