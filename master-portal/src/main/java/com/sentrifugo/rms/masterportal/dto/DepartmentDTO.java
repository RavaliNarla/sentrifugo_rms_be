package com.sentrifugo.rms.masterportal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Department master: name plus an optional description. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {
    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
}
