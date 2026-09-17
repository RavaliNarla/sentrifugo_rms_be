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

    /** Manually-entered on the position itself - distinct from the Position Master's own copy (see Section 6 of the requirements doc). */
    private String rolesResponsibilities;

    private UUID educationQualificationId;
    private String educationQualificationName;

    /** Optional - see SpecializationDTO. Only meaningful when educationQualificationId is set. */
    private UUID specializationId;
    private String specializationName;

    private Integer experienceYears;

    /** Optional, non-mandatory per the requirements doc - free text, useful for roles requiring certifications. */
    private String certifications;

    private Boolean medicalFitnessRequired;

    /** Only applicable/shown when employmentType = CONTRACT. */
    private String contractualPeriod;

    private String employmentType;

    private Integer vacancies;

    private String approvalDocUrl;

    private UUID approvedById;
    private String approvedByName;

    /** Free-text approver name, used when the selected Approved-By role is "Others". */
    private String approvedByOtherText;

    private LocalDate approvedOn;

    private String status;
}
