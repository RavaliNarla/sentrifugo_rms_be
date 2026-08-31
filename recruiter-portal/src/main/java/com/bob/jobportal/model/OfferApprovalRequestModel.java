package com.bob.jobportal.model;

import com.bob.db.enums.CompensationStatus;
import com.bob.db.enums.OfferApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OfferApprovalRequestModel {
    private String searchText;
    @NotNull(message = "Position ID cannot be null")
    private List<UUID> positionIds;
    private List<OfferApprovalStatus> statusList;
    private int page = 0;
    private int size = 5;
}
