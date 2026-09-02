package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.enums.OfferStatus;
import com.sentrifugo.rms.db.enums.RequisitionStatus;
import com.sentrifugo.rms.db.repository.CandidateOfferRepository;
import com.sentrifugo.rms.db.repository.CandidateRepository;
import com.sentrifugo.rms.db.repository.JobRequisitionRepository;
import com.sentrifugo.rms.recruiterportal.dto.DashboardSummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("${recruiter.api.base.path}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final JobRequisitionRepository jobRequisitionRepository;
    private final CandidateRepository candidateRepository;
    private final CandidateOfferRepository candidateOfferRepository;

    @Operation(summary = "Simple counts for the dashboard landing page")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> summary() {
        long total = jobRequisitionRepository.count();
        long pending = jobRequisitionRepository.findByStatusIn(
                java.util.List.of(RequisitionStatus.L1_PENDING, RequisitionStatus.L2_PENDING)).size();
        long approved = jobRequisitionRepository.findByStatus(RequisitionStatus.APPROVED).size();
        long fulfilled = jobRequisitionRepository.findByStatus(RequisitionStatus.FULFILLED).size();

        long totalCandidates = candidateRepository.count();
        long shortlisted = candidateRepository.findAll().stream().filter(c -> c.getStatus() == CandidateStatus.SHORTLISTED).count();
        long scheduled = candidateRepository.findAll().stream().filter(c -> c.getStatus() == CandidateStatus.SCHEDULED).count();
        long qualified = candidateRepository.findAll().stream().filter(c -> c.getStatus() == CandidateStatus.QUALIFIED).count();
        long offersSent = candidateOfferRepository.findAll().stream().filter(o -> o.getStatus() == OfferStatus.SENT || o.getStatus() == OfferStatus.ACCEPTED).count();

        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .totalRequisitions(total)
                .pendingApprovalRequisitions(pending)
                .approvedRequisitions(approved)
                .fulfilledRequisitions(fulfilled)
                .totalCandidates(totalCandidates)
                .shortlistedCandidates(shortlisted)
                .scheduledCandidates(scheduled)
                .qualifiedCandidates(qualified)
                .offersSent(offersSent)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(summary, "Dashboard summary fetched successfully"));
    }
}
