package com.bob.jobportal.model;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateRequisitionEditDraftRequestModel {
    private List<UUID> positionIds;
}
