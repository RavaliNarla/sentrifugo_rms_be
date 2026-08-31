package com.bob.jobportal.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MeritListSummary {
    private VacancyMatrixModel vacancyMatrix;
    private List<com.bob.db.model.MeritCandidateDTO> meritCandidates;
}
