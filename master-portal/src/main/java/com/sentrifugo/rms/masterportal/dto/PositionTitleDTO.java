package com.sentrifugo.rms.masterportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Position Master DTO - richer than the plain id+name NamedMasterDTO. Carries the
 * department link (drives the department-filtered dropdown on Add Position) and
 * the prefill fields (Job Description, Minimum Experience).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionTitleDTO {
    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Department is required")
    private UUID departmentId;
    private String departmentName;

    private String jobDescription;

    private Integer minimumExperienceYears;
}
