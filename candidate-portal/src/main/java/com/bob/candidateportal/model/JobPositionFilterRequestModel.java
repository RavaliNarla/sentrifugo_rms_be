package com.bob.candidateportal.model;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class JobPositionFilterRequestModel {
    private String searchText;
    private UUID candidateId;
    private List<UUID> deptIds;
    private List<UUID> stateIds;
    private Integer monthMinExp;
    private Integer monthMaxExp;
    private UUID requisitionId;
    private int page = 0; // Default to first page
    private int size = 10; // Default page size
}