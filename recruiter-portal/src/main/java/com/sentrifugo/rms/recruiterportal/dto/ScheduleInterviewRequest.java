package com.sentrifugo.rms.recruiterportal.dto;

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
public class ScheduleInterviewRequest {

    @NotEmpty(message = "Select at least one candidate")
    private List<UUID> candidateIds;

    @NotEmpty(message = "Add at least one panel window")
    private List<PanelWindowDTO> panelWindows;
}
