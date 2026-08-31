package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CandidateResumeUploadService;
import com.bob.commonutil.model.candidateportal.ResumeModel;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import com.bob.db.entity.CandidateDocumentStoreEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/resume")
public class CandidateResumeUploadController {

    @Autowired
    private CandidateResumeUploadService candidateResumeUploadService;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeModel>> uploadResume( @RequestParam("resume") MultipartFile resume) throws IOException {
        UUID candidateId = securityUtils.getCurrentUserId();
        ResumeModel resumeModel = candidateResumeUploadService.uploadResume(candidateId,resume);
        return ResponseEntity.ok(ApiResponse.ok(resumeModel, "Resume uploaded successfully"));
    }

    @GetMapping("/get-resume-details")
    public ResponseEntity<ApiResponse<CandidateDocumentStoreEntity>> getResume(){
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateDocumentStoreEntity resumeModel = candidateResumeUploadService.getResumeDetaile(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(resumeModel, "Resume fetched successfully"));
    }
}
