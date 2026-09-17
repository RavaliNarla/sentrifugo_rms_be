package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.CandidateOfferDTO;
import com.sentrifugo.rms.recruiterportal.dto.GenerateOfferRequest;
import com.sentrifugo.rms.recruiterportal.dto.OfferApprovalActionRequest;
import com.sentrifugo.rms.recruiterportal.dto.OfferPreviewRequest;
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

    @Operation(summary = "Generate offer letter(s) from a template as a draft (not yet sent - must go through L1/L2 approval first)")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<CandidateOfferDTO>>> generate(@Valid @RequestBody GenerateOfferRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(offerService.generateOffers(request), "Offer(s) generated successfully"));
    }

    @Operation(summary = "Preview the offer letter HTML for a candidate/template without saving or sending anything")
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<String>> preview(@Valid @RequestBody OfferPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(offerService.previewOffer(request), "Offer preview rendered successfully"));
    }

    @Operation(summary = "Submit generated offer(s) for L1 approval")
    @PostMapping("/submit-for-approval")
    public ResponseEntity<ApiResponse<Void>> submitForApproval(@RequestBody List<UUID> candidateIds) {
        offerService.submitForApproval(candidateIds);
        return ResponseEntity.ok(ApiResponse.ok("Offer(s) submitted for approval"));
    }

    @Operation(summary = "L1 or L2 approve/reject offer(s) (role resolved from the caller's approver record). L2 approval automatically emails the candidate.")
    @PostMapping("/approve-reject")
    public ResponseEntity<ApiResponse<Void>> approveOrReject(@RequestBody OfferApprovalActionRequest request) {
        offerService.approveOrReject(request);
        return ResponseEntity.ok(ApiResponse.ok(request.isApprove() ? "Offer(s) approved" : "Offer(s) rejected"));
    }

    @Operation(summary = "List offers pending the current user's L1/L2 approval - for the Admin > Offer Approvals screen")
    @GetMapping("/pending-approval")
    public ResponseEntity<ApiResponse<List<CandidateOfferDTO>>> getPendingApprovals() {
        return ResponseEntity.ok(ApiResponse.ok(offerService.getPendingApprovals(), "Pending offer approvals fetched successfully"));
    }

    @Operation(summary = "Get the offer for a candidate (to show the doc icon + preview)")
    @GetMapping("/by-candidate/{candidateId}")
    public ResponseEntity<ApiResponse<CandidateOfferDTO>> getByCandidate(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(ApiResponse.ok(offerService.getByCandidateId(candidateId), "Offer fetched successfully"));
    }
}
