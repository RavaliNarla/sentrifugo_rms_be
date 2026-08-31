package com.bob.candidateportal.model;

import com.bob.db.enums.CompensationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ApplicationCompensationModel {
    private UUID applicationId;
    private BigDecimal currentCtc;
    private BigDecimal expectedCtc;
    private LocalDate submitBeforeDate;
    private CompensationStatus compensationStatus;

}
