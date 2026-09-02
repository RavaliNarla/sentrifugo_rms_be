package com.sentrifugo.rms.masterportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Location name is required")
    private String name;

    @NotNull(message = "State is required")
    private UUID stateId;

    private String stateName;

    private String address;
}
