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
public class LocationDTO {
    private UUID id;

    /** HRMS business-unit id (sync key). */
    private String hrmsId;

    @NotBlank(message = "Location name is required")
    private String name;

    @NotBlank(message = "Code is required")
    private String code;

    /** Optional — kept for compatibility; not shown in Admin UI. */
    private UUID stateId;

    private String stateName;

    private String address;
}
