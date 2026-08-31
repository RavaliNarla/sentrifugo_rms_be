package com.bob.masterdata.Model;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class InterviewCenterRequestModel {
    private List<String> organizationTypes;
    private UUID zonalStateId;
}
