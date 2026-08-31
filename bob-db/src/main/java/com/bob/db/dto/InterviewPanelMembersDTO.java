package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewPanelMembersDTO extends BaseDTO implements Serializable {

    @JsonProperty("interviewPanelMemberId")
    private UUID id;

    private UUID panelId;

    private UserDTO panelMember;
}
