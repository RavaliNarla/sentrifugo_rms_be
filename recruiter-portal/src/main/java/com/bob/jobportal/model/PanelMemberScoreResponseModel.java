package com.bob.jobportal.model;

import com.bob.db.dto.PanelMembersScoreDTO;
import com.bob.db.dto.UserDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PanelMemberScoreResponseModel {

    private PanelMembersScoreDTO panelMembersScore;
    private UserDTO user;
}
