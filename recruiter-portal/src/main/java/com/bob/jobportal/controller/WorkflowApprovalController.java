package com.bob.jobportal.controller;

import com.bob.commonutil.model.WorkflowApprStatusResponseModel;
import com.bob.commonutil.service.WorkflowApprovalService;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.WorkflowApprovalDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/workflow-approval")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class WorkflowApprovalController {

    @Autowired
    private WorkflowApprovalService workflowApprovalService;

    @GetMapping("/get-requisition-approval-history/{requisitionId}")
    public ApiResponse<List<WorkflowApprovalDTO>> getReqApprovalHistoryByRequisitionId(@PathVariable("requisitionId") UUID requisitionId) {
        return ApiResponse.ok(workflowApprovalService.getReqApprovalHistoryByRequisitionId(requisitionId), "Position approval history fetched successfully");
    }

    @GetMapping("/get-panels-approval-history/{panelId}")
    public ApiResponse<List<WorkflowApprovalDTO>> getPanelApprovalHistory(@PathVariable("panelId") UUID panelId) {
        return ApiResponse.ok(workflowApprovalService.getPanelApprovalHistory(panelId), "Position Panels approval history fetched successfully");
    }

    @GetMapping("/get-conversation-threads-approval-history/{conversationThreadId}")
    public ApiResponse<List<WorkflowApprovalDTO>> getConversationThreadApprovalHistory(@PathVariable("conversationThreadId") UUID conversationThreadId) {
        return ApiResponse.ok(workflowApprovalService.getConversationThreadApprovalHistory(conversationThreadId), "Conversation threads history fetched successfully");
    }

    @GetMapping("/get-draft-requisition-approval-history/{draftId}")
    public ApiResponse<List<WorkflowApprovalDTO>> getDraftRequisitionApprovalHistory(@PathVariable("draftId") UUID draftId) {
        return ApiResponse.ok(workflowApprovalService.getDraftRequisitionApprovalHistory(draftId), "Draft requisition approval history fetched successfully");
    }

    @GetMapping("/get-requisition-approval-history-including-drafts/{requisitionId}")
    public ApiResponse<List<WorkflowApprovalDTO>> getRequisitionApprovalHistoryIncludingDrafts(@PathVariable("requisitionId") UUID requisitionId) {
        return ApiResponse.ok(workflowApprovalService.getRequisitionApprovalHistoryIncludingDrafts(requisitionId), "Requisition approval history including drafts fetched successfully");
    }
}
