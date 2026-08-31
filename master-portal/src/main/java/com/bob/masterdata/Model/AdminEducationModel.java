package com.bob.masterdata.Model;

import com.bob.db.dto.EducationGroupsDTO;
import com.bob.db.dto.EducationQualificationsDTO;
import com.bob.db.dto.SpecializationMasterDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AdminEducationModel {

    @NotNull(message="Qualification can't be empty")
    private EducationQualificationsDTO qualification;

    private List<SpecializationGroupingModel> specializations;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecializationGroupingModel {

        private SpecializationMasterDTO specialization;

        private EducationGroupsDTO group;
    }
}

