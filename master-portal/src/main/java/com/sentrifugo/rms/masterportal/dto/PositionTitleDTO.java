package com.sentrifugo.rms.masterportal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Position Master DTO. Department link and experience / R&R are RMS-only and optional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionTitleDTO {
    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;

    /** Null = None (unlinked; appears under every department on Add Position). */
    private UUID departmentId;
    private String departmentName;

    private String jobDescription;

    private Integer minimumExperienceYears;
}
