package com.bob.jobportal.model;

import com.bob.db.dto.WrittenExamConfigurationDTO;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ExamConfigSaveRequestModel {
    private List<UUID> positionIds;
    private WrittenExamConfigurationDTO config;
}
