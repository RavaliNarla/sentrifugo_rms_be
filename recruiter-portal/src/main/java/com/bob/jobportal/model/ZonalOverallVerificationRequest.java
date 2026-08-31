package com.bob.jobportal.model;

import com.bob.db.enums.ZonalVerificationStatus;
import com.bob.db.enums.LptStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZonalOverallVerificationRequest {
    
    @NotNull(message = "Candidate Id is required")
    private UUID candidateId;

    @NotNull(message = "Application Id is required")
    private UUID applicationId;

    @NotNull(message = "Interview Schedule Id is required")
    private UUID interviewScheduleId;

    @NotNull(message = "Overall verification status is required")
    private ZonalVerificationStatus zonalVerificationStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate zonalSubmitBeforeDate;

    private String zonalHrComments;

    private Boolean lptRequired;

    private LptStatus lptStatus;
}
