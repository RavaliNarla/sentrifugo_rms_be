package com.bob.jobportal.model;

import com.bob.db.enums.InterviewSchedulingStatus;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class InterviewedCandidateRequestModel {

    private String searchText;
    private List<UUID> positionIds;
    private List<InterviewSchedulingStatus> statusList;
    private int page = 0; // Default to first page
    private int size = 5; // Default page size
}
