package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class InterviewPanelsDTO extends BaseDTO implements Serializable {

    @JsonProperty("interviewPanelId")
    private UUID id;

    @NotBlank(message = "Panel name is required")
    private String panelName;

    private String description;

    @Valid
    private InterviewCommitteeDTO committee;

    @Valid
    private List<InterviewPanelMembersDTO> panelMembers;
}
