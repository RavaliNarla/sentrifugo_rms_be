package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.CandidateDTO;
import com.sentrifugo.rms.recruiterportal.dto.CompensationDetailsRequest;
import com.sentrifugo.rms.recruiterportal.service.CompensationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Compensation Management")
@RestController
@RequestMapping("${recruiter.api.base.path}/compensation-pool")
@RequiredArgsConstructor
public class CompensationController {

    private final CompensationService compensationService;

    @Operation(summary = "Move QUALIFIED candidates into Compensation Management")
    @PostMapping("/move-in")
    public ResponseEntity<ApiResponse<Void>> moveIn(@RequestBody List<UUID> candidateIds) {
        compensationService.moveToCompensation(candidateIds);
        return ResponseEntity.ok(ApiResponse.ok("Candidate(s) moved to Compensation Management"));
    }

    @Operation(summary = "Set/update a candidate's compensation details (Current/Expected CTC, Fixed/Variable Pay, Bonus, Agreed CTC)")
    @PutMapping("/{candidateId}/details")
    public ResponseEntity<ApiResponse<Void>> updateDetails(@PathVariable UUID candidateId, @RequestBody CompensationDetailsRequest request) {
        compensationService.updateCompensationDetails(candidateId, request);
        return ResponseEntity.ok(ApiResponse.ok("Compensation details updated successfully"));
    }

    @Operation(summary = "Move candidates (with Agreed CTC filled) into the Offer Pool")
    @PostMapping("/move-to-offer")
    public ResponseEntity<ApiResponse<Void>> moveToOffer(@RequestBody List<UUID> candidateIds) {
        compensationService.moveToOffer(candidateIds);
        return ResponseEntity.ok(ApiResponse.ok("Candidate(s) moved to Offer Pool"));
    }

    @Operation(summary = "List Compensation Management candidates for a position (server-side paginated)")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<CandidateDTO>>> search(
            @RequestParam UUID positionId,
            @RequestParam(required = false, defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                compensationService.getCompensationPool(positionId, searchText, page, size),
                "Compensation pool fetched successfully"));
    }
}
