package com.bob.db.dto;

import com.bob.db.enums.RegexPattern;
import com.bob.db.enums.RequisitionStatus;
import com.bob.db.util.excel.RegexValidate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;
import java.io.Serializable;

@Data
public class JobRequisitionsDTO extends BaseDTO implements Serializable {

    private UUID id;

    @NotBlank(message = "Requisition title is required")
    @RegexValidate(regex = RegexPattern.REQ_TITLE_PATTERN,message = "Only letters, numbers, spaces, and the special characters . , - _ / ( ) & : ; ' \" @ # are allowed.")
    private String requisitionTitle;

    @RegexValidate(regex = RegexPattern.REQ_DESCRIPTION_PUNCTUATION,message = "Only letters, numbers, spaces, and the special characters . , - _ / ( ) & : ; ' \" @ # % + are allowed.")
    private String requisitionDescription;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private RequisitionStatus requisitionStatus = RequisitionStatus.NEW;

    private String requisitionComments;

    private String requisitionCode;
    
    // Aggregated counts from job_positions
    private Integer departmentCount;
    
    private Integer positionCount;
    
    private Integer vacancyCount;

    private boolean hasDraftPositions = false;

    private UUID parentRequisitionId;

    private Boolean isReinitialized = false;

    private LocalDate cutoffDate;

    private Boolean isHiringCompleted;

}
