package com.bob.jobportal.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class JobPositionExclusionsEditRequestModel {

    private UUID exclusionId;

    private Boolean isExcluded = false;
}
