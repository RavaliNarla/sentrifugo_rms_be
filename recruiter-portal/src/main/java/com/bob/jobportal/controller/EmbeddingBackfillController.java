package com.bob.jobportal.controller;

import com.bob.commonutil.service.EmbeddingBuilderService;
import com.bob.db.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("${recruiter.api.base.path}/embeddings")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER')")
public class EmbeddingBackfillController {

    @Autowired
    private EmbeddingBuilderService embeddingBuilderService;

    @PostMapping("/backfill-missing-scores")
    public ResponseEntity<ApiResponse<Map<String, Object>>> backfillMissingScores() {
        Map<String, Object> result = embeddingBuilderService.backfillMissingRankingResults();
        return ResponseEntity.ok(ApiResponse.ok(result, "Missing ranking scores backfill completed"));
    }
}
