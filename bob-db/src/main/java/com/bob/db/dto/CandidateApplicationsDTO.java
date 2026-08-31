package com.bob.db.dto;

import com.bob.db.enums.ApplicationPaymentStatus;
import com.bob.db.enums.CandidateApplicationStatus;
import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
import java.io.Serializable;

@Data

public class CandidateApplicationsDTO extends BaseDTO implements Serializable {
    private UUID id;
    private CandidateApplicationStatus applicationStatus = CandidateApplicationStatus.APPLIED;
    private LocalDateTime applicationDate;
    private LocalDateTime updatedDate;
    private UUID candidateId;
    private UUID positionId;
    private String applicationNo;
    private Boolean isAbsent = false;
    private String statusReason;
    private ApplicationPaymentStatus paymentStatus=ApplicationPaymentStatus.PENDING;
    private UUID examCenterId;
    private Integer stepper;
}
