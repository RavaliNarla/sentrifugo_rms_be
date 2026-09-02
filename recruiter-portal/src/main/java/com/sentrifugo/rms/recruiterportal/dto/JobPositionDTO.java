package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPositionDTO {
    private UUID id;
    private UUID requisitionId;

    @NotNull(message = "Department is required")
    private UUID departmentId;
    private String departmentName;

    @NotNull(message = "Location is required")
    private UUID locationId;
    private String locationName;

    @NotNull(message = "Position title is required")
    private UUID positionTitleId;
    private String positionTitleName;

    private String jobDescription;

    private UUID educationQualificationId;
    private String educationQualificationName;

    private Integer experienceYears;

    private String employmentType;

    private Integer vacancies;

    private String approvalDocUrl;

    private UUID approvedById;
    private String approvedByName;

    private LocalDate approvedOn;

    private String status;
}
