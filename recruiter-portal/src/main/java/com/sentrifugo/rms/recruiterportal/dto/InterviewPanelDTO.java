package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewPanelDTO {
    private UUID id;

    @NotBlank(message = "Panel name is required")
    private String name;

    @NotEmpty(message = "At least one panel member is required")
    private List<UUID> memberIds;

    private List<String> memberNames;
}
