package com.bob.candidateportal.controllers;

import com.bob.commonutil.service.WorkflowApprovalService;
import com.bob.commonutil.model.WorkflowApprStatusResponseModel;
import com.bob.db.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/track-app-status")
public class WorkflowApprovalController {

    @Autowired
    private WorkflowApprovalService workflowApprovalService;

    @GetMapping("/status/{applicationId}")
    public ApiResponse<List<WorkflowApprStatusResponseModel>> getApplicationStatus(@PathVariable("applicationId") UUID applicationId) {
        return ApiResponse.ok(workflowApprovalService.getApplicationStatus(applicationId), "Application status fetched successfully");
    }
}
