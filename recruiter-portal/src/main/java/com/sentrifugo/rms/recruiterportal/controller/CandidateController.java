package com.sentrifugo.rms.recruiterportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.recruiterportal.dto.CandidateBulkImportResult;
import com.sentrifugo.rms.recruiterportal.dto.CandidateDTO;
import com.sentrifugo.rms.recruiterportal.dto.ShortlistDecisionRequest;
import com.sentrifugo.rms.recruiterportal.service.CandidateBulkImportService;
import com.sentrifugo.rms.recruiterportal.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Candidate Pool")
@RestController
@RequestMapping("${recruiter.api.base.path}/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;
    private final CandidateBulkImportService candidateBulkImportService;
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

    @Operation(summary = "Edit a candidate (only while status = ADDED)")
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<CandidateDTO>> update(
            @PathVariable UUID id,
            @RequestPart("candidate") String candidateJson,
            @RequestPart(value = "resume", required = false) MultipartFile resume,
            @RequestPart(value = "idProof", required = false) MultipartFile idProof,
            @RequestPart(value = "photo", required = false) MultipartFile photo) throws Exception {
        CandidateDTO dto = objectMapper.readValue(candidateJson, CandidateDTO.class);
        return ResponseEntity.ok(ApiResponse.ok(candidateService.update(id, dto, resume, idProof, photo), "Candidate updated successfully"));
    }

    @Operation(summary = "Delete a candidate (only while status = ADDED)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        candidateService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Candidate deleted successfully"));
    }

    @Operation(summary = "Delete one candidate document: photo | resume | id-proof (only while status = ADDED)")
    @DeleteMapping("/{id}/documents/{documentType}")
    public ResponseEntity<ApiResponse<CandidateDTO>> deleteDocument(
            @PathVariable UUID id,
            @PathVariable String documentType) {
        return ResponseEntity.ok(ApiResponse.ok(
                candidateService.deleteDocument(id, documentType),
                "Document deleted successfully"));
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

    @Operation(summary = "Shortlist a candidate (ADDED -> SHORTLISTED). Kept for backward compatibility - prefer /decision.")
    @PostMapping("/{id}/shortlist")
    public ResponseEntity<ApiResponse<Void>> shortlist(@PathVariable UUID id) {
        candidateService.decide(id, ShortlistDecisionRequest.Decision.SHORTLIST);
        return ResponseEntity.ok(ApiResponse.ok("Candidate shortlisted successfully"));
    }

    @Operation(summary = "Record shortlist decision: SHORTLIST / REJECT / HOLD → SHORTLISTED / REJECTED / ON_HOLD")
    @PostMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<Void>> decide(@PathVariable UUID id, @Valid @RequestBody ShortlistDecisionRequest request) {
        candidateService.decide(id, request.getDecision());
        return ResponseEntity.ok(ApiResponse.ok("Decision recorded successfully"));
    }

    @Operation(summary = "Download XLSX template for bulk candidate import (Name, Phone, Email)")
    @GetMapping("/bulk-template")
    public ResponseEntity<byte[]> downloadBulkTemplate() {
        byte[] bytes = candidateBulkImportService.buildTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"candidate-bulk-template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @Operation(summary = "Bulk-add candidates for a position from an XLSX file (Name, Phone, Email columns)")
    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateBulkImportResult>> bulkImport(
            @RequestParam UUID positionId,
            @RequestPart("file") MultipartFile file) {
        CandidateBulkImportResult result = candidateBulkImportService.importCandidates(positionId, file);
        String message = result.getSuccessCount() + " candidate(s) imported"
                + (result.getFailureCount() > 0 ? ", " + result.getFailureCount() + " failed" : "");
        return ResponseEntity.ok(ApiResponse.ok(result, message));
    }
}
