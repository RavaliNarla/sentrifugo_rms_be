package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.OfferApprovalHistoryDTO;
import com.bob.jobportal.model.ApproveRejectModel;
import com.bob.jobportal.model.OfferApprovalHistoryModel;
import com.bob.jobportal.model.OfferApprovalRequestModel;
import com.bob.jobportal.service.OfferApprovalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("${recruiter.api.base.path}/offer-approval")
public class OfferApprovalController {

    @Autowired
    private OfferApprovalService offerApprovalService;


    @PostMapping("/approve-or-reject")
    public ResponseEntity<ApiResponse<Object>> approveOrRejectCandidate(@Valid @RequestBody ApproveRejectModel approveRejectModel){
        offerApprovalService.approveOrRejectCandidate(approveRejectModel);
        return ResponseEntity.ok(ApiResponse.ok("Candidates Approved/Rejected successfully!"));
    }

    @PostMapping("/candidates/search")
    public ResponseEntity<ApiResponse<Object>> searchPendingCandidates(@RequestBody OfferApprovalRequestModel requestModel) {
        Page<OfferApprovalHistoryModel> candidates = offerApprovalService.fetchPendingApprovals(requestModel);
        return ResponseEntity.ok(ApiResponse.ok(candidates, "Approval candidates fetched successfully"));
    }

    @GetMapping("/workflow-history/{historyId}")
    public ResponseEntity<ApiResponse<?>> getWorkflowHistory(@PathVariable UUID historyId) {
        return ResponseEntity.ok(ApiResponse.ok(offerApprovalService.getWorkflowHistory(historyId), "Workflow history fetched successfully"));
    }


}
