package com.sentrifugo.rms.masterportal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Shared shape for the simple id+name master lists (Position Titles, Education
 * Qualifications, Approved-By roles, Offer Templates use their own richer DTO).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedMasterDTO {
    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;
}
