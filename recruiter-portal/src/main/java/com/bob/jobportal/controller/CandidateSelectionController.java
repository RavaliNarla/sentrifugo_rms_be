package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.JobPositionsDTO;
import com.bob.db.mapper.JobPositionsMapper;
import com.bob.db.repository.PositionsRepository;
import com.bob.db.model.MeritCandidateDTO;
import com.bob.jobportal.model.MeritListSummary;
import com.bob.jobportal.model.VacancyMatrixModel;
import com.bob.jobportal.service.CandidateSelectionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/candidate-selection")
public class CandidateSelectionController {

    @Autowired
    private CandidateSelectionService candidateSelectionService;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private JobPositionsMapper jobPositionsMapper;

    @GetMapping("/candidate/{positionId}")
    public List<MeritCandidateDTO> loadCandidateSelection(@PathVariable UUID positionId){
     return candidateSelectionService.loadCandidateSelection(positionId);
    }

    @GetMapping("/Vacancy/{positionId}")
    @Transactional(readOnly = true)
    public VacancyMatrixModel loadVacancyMatrix(UUID PositionId) {
        JobPositionsDTO jobPosition = jobPositionsMapper.toDto(positionsRepository.findById(PositionId).orElseThrow(() -> new RuntimeException("Position not found")));
        return candidateSelectionService.loadVacancyMatrix(jobPosition);
    }

    @GetMapping("/merit-list/{positionId}")
    public ResponseEntity<ApiResponse<MeritListSummary>> loadMeritList(@PathVariable UUID positionId) {
        return ResponseEntity.ok(ApiResponse.ok(candidateSelectionService.loadMeritList(positionId),"Select list calculated successfully"));
    }


}
