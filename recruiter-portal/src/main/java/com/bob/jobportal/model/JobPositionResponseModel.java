package com.bob.jobportal.model;

import com.bob.db.dto.JobPositionsDTO;
import com.bob.db.dto.MasterPositionsDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobPositionResponseModel {

    private JobPositionsDTO jobPositions;
    private MasterPositionsDTO masterPositions;
}
