package com.bob.jobportal.model;

import com.bob.db.entity.CandidateRankingResultsEntity;
import com.bob.db.enums.CandidateApplicationStatus;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CandidateDetailsFilterRequestModel {
    // pagination Model Default
    private String searchText;
    private int page = 0; // Default to first page
    private int size = 5; // Default page size

    // filter model
    private List<UUID> positionIds;
    private List<CandidateApplicationStatus> status;
    private UUID stateId;
    private UUID categoryId;
    private Boolean rank =false;
}
