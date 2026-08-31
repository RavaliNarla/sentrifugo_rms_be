package com.bob.jobportal.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RequisitionEditPositionResponseModel {
    private UUID positionEditRequestId;
    private UUID parentPositionId;
    private UUID masterPositionId;
    private UUID deptId;
    private Integer totalVacancies;
    private Boolean isLocationWise;
}
