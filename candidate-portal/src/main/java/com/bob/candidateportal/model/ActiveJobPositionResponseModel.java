package com.bob.candidateportal.model;


import com.bob.db.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveJobPositionResponseModel {

    private JobPositionsDTO positionsDTO;
    private MasterPositionsDTO masterPositionsDTO;
    private JobRequisitionsDTO requisitionsDTO;
    private List<PositionStateDistributionDTO> positionStateDistributions;

}
