package com.bob.db.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequisitionResponseDTO {
    
    private UUID id;
    
    private String requisitionCode;
    
    private String requisitionTitle;
    
    private String requisitionDescription;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private String requisitionStatus;
    
    private String requisitionComments;
    
    private String indentPath;
    
    private LocalDateTime createdDate;
    
    private UUID createdBy;
    
    // Aggregated counts from job_positions
    private Integer departmentCount;
    
    private Integer positionCount;
    
    private Integer vacancyCount;
}
