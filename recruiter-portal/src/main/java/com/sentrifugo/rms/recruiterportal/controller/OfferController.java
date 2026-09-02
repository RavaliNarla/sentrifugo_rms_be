package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.CandidateOfferDTO;
import com.sentrifugo.rms.recruiterportal.dto.GenerateOfferRequest;
import com.sentrifugo.rms.recruiterportal.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Offer Pool")
@RestController
@RequestMapping("${recruiter.api.base.path}/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @Operation(summary = "Generate offer letter(s) from a template and immediately email them with Accept/Reject links")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<CandidateOfferDTO>>> generate(@Valid @RequestBody GenerateOfferRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(offerService.generateAndSendOffers(request), "Offer(s) generated and sent successfully"));
    }

    @Operation(summary = "Get the offer for a candidate (to show the doc icon + preview)")
    @GetMapping("/by-candidate/{candidateId}")
    public ResponseEntity<ApiResponse<CandidateOfferDTO>> getByCandidate(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(ApiResponse.ok(offerService.getByCandidateId(candidateId), "Offer fetched successfully"));
    }
}
