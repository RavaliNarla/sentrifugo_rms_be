package com.sentrifugo.rms.recruiterportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.recruiterportal.dto.CandidateDTO;
import com.sentrifugo.rms.recruiterportal.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Tag(name = "Candidate Pool")
@RestController
@RequestMapping("${recruiter.api.base.path}/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Add a candidate manually to a position's Candidate Pool (multipart: 'candidate' JSON + optional 'resume' + optional 'idProof' + optional 'photo')")
    @PostMapping(value = "/add", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<CandidateDTO>> add(
            @RequestPart("candidate") String candidateJson,
            @RequestPart(value = "resume", required = false) MultipartFile resume,
            @RequestPart(value = "idProof", required = false) MultipartFile idProof,
            @RequestPart(value = "photo", required = false) MultipartFile photo) throws Exception {
        CandidateDTO dto = objectMapper.readValue(candidateJson, CandidateDTO.class);
        return ResponseEntity.ok(ApiResponse.ok(candidateService.add(dto, resume, idProof, photo), "Candidate added successfully"));
    }

    @Operation(summary = "Search/list candidates for a position (server-side paginated)")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<CandidateDTO>>> search(
            @RequestParam UUID positionId,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false, defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<CandidateStatus> statusList = statuses == null || statuses.isEmpty() ? null :
                statuses.stream().map(CandidateStatus::valueOf).toList();
        return ResponseEntity.ok(ApiResponse.ok(
                candidateService.search(positionId, statusList, searchText, page, size),
                "Candidates fetched successfully"));
    }

    @Operation(summary = "Get a candidate's basic profile details")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateDTO>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(candidateService.getById(id), "Candidate fetched successfully"));
    }

    @Operation(summary = "Shortlist a candidate (ADDED -> SHORTLISTED)")
    @PostMapping("/{id}/shortlist")
    public ResponseEntity<ApiResponse<Void>> shortlist(@PathVariable UUID id) {
        candidateService.shortlist(id);
        return ResponseEntity.ok(ApiResponse.ok("Candidate shortlisted successfully"));
    }
}
