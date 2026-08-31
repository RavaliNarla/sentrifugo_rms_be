package com.bob.jobportal.model;

import com.bob.db.dto.PositionPanelDTO;
import lombok.Data;

import java.util.List;

@Data
public class PositionPanelResponseModel {
    List<PositionPanelDTO> interviewPanelList;
    List<PositionPanelDTO> screeningPanelList;
    List<PositionPanelDTO> compensationPanelList;
}
