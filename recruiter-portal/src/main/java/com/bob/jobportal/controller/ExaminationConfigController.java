package com.bob.jobportal.controller;

import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.WrittenExamConfigurationDTO;
import com.bob.jobportal.model.ExamConfigApprovalRequestModel;
import com.bob.jobportal.model.ExamConfigSaveRequestModel;
import com.bob.jobportal.service.ExaminationConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/examination-config")
public class ExaminationConfigController {

    @Autowired
    private ExaminationConfigService examinationConfigService;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping("/get-by-positions")
    public ResponseEntity<ApiResponse<?>> getExamConfigsByPositionIds(@RequestParam("positionIds") List<UUID> positionIds) {
        return ResponseEntity.ok(ApiResponse.ok(examinationConfigService.getExamConfigsByPositionIds(positionIds), "Exam configurations fetched successfully"));
    }

    @GetMapping("/get-all-list")
    public ResponseEntity<ApiResponse<?>> getAllExamConfigs(@RequestParam List<UUID> requisitionIds) {
        return ResponseEntity.ok(ApiResponse.ok(examinationConfigService.getAllExamConfigs(requisitionIds), "Exam configurations fetched successfully"));
    }

    @PostMapping("/save-exam-config")
    public ResponseEntity<ApiResponse<?>> saveExamConfig(@RequestBody ExamConfigSaveRequestModel request) {
        return ResponseEntity.ok(ApiResponse.ok(examinationConfigService.saveOrUpdateExamConfig(request), "Exam configuration saved successfully"));
    }

    @PostMapping("/approve-or-reject")
    public ResponseEntity<ApiResponse<?>> approveOrReject(@RequestBody ExamConfigApprovalRequestModel request) {
        WrittenExamConfigurationDTO updated = examinationConfigService.approveOrReject(request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Exam configuration status updated successfully"));
    }
    
    @PostMapping("/submit-for-approval")
    public ResponseEntity<ApiResponse<List<WrittenExamConfigurationDTO>>> submitForApproval(@RequestBody List<UUID> positionIds) {
        return ResponseEntity.ok(ApiResponse.ok(examinationConfigService.submitForApproval(positionIds), "Exam configurations submitted for approval successfully"));
    }

    @GetMapping("/workflow-history/{examConfigId}")
    public ResponseEntity<ApiResponse<?>> getWorkflowHistory(@PathVariable UUID examConfigId) {
        return ResponseEntity.ok(ApiResponse.ok(examinationConfigService.getWorkflowHistory(examConfigId), "Workflow history fetched successfully"));
    }

    @PostMapping("/finalize")
    public ResponseEntity<ApiResponse<?>> finalizeExamConfigs(@RequestBody List<UUID> positionIds) {
        return ResponseEntity.ok(ApiResponse.ok(examinationConfigService.finalizeExamConfigs(positionIds), "Exam configurations finalized successfully"));
    }

    @GetMapping("/validate-exam-configuration")
    public ResponseEntity<ApiResponse<Boolean>> validateExamConfiguration(@RequestParam("positionId") UUID positionId) {
        return ResponseEntity.ok(ApiResponse.ok(examinationConfigService.validateExamConfiguration(positionId), "Exam configuration validation completed successfully"));
    }

    @DeleteMapping("/delete-section/{sectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteExamSection(@PathVariable UUID sectionId) {
        examinationConfigService.deleteExamSection(sectionId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Exam section deleted successfully"));
    }

}
