package com.sentrifugo.rms.masterportal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecializationDTO {
    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;

    /** Optional - null means this specialization is general / not tied to one education level. */
    private UUID educationQualificationId;
    private String educationQualificationName;
}
