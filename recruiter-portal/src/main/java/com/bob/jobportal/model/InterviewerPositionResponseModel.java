package com.bob.jobportal.model;

import com.bob.db.dto.JobPositionsDTO;
import com.bob.db.dto.JobRequisitionsDTO;
import com.bob.db.dto.MasterPositionsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewerPositionResponseModel {
    
    private JobPositionsDTO position;
    
    private MasterPositionsDTO masterPosition;
    
    private JobRequisitionsDTO requisition;
}
