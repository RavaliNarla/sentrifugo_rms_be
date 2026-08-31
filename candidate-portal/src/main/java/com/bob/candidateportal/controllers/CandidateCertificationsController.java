package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CandidateCertificationsService;
import com.bob.db.model.CandidateCertificationsResponseModel;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CandidateCertificationsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/certifications")
public class CandidateCertificationsController {
    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CandidateCertificationsService candidateCertificationsService;

    @PostMapping(value = "/save-cert-details",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> saveCandidateCertifications(
                                                                      @RequestPart(value = "file", required = false) MultipartFile file,
                                                                      @RequestPart CandidateCertificationsDTO certificationsDTO) throws IOException {
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateCertificationsResponseModel res = candidateCertificationsService.saveCandidateCertifications(candidateId, certificationsDTO, file);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate certifications details saved successfully"));
    }

    @GetMapping("/get-cert-details")
    public ResponseEntity<ApiResponse<?>> getCandidateCertifications()  {
        UUID candidateId = securityUtils.getCurrentUserId();
        List<CandidateCertificationsResponseModel> res = candidateCertificationsService.getCandidateCertifications(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate certifications details fetched successfully"));
    }

    @DeleteMapping("/delete-cert-details/{certificationId}")
    public ResponseEntity<ApiResponse<?>> deleteCandidateCertification( @PathVariable UUID certificationId) {
        candidateCertificationsService.deleteCandidateCertification( certificationId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Candidate certification deleted successfully"));
    }

    @PostMapping("/save-has-cert")
    public ResponseEntity<ApiResponse<?>> saveHasCertification( @RequestParam Boolean hasCertification) {
        UUID candidateId = securityUtils.getCurrentUserId();
        Boolean res = candidateCertificationsService.saveHasCertification(candidateId, hasCertification);
        return ResponseEntity.ok(ApiResponse.ok(res, "Has certification saved successfully"));
    }

    @GetMapping("/get-has-cert")
    public ResponseEntity<ApiResponse<?>> getHasCertification() {
        UUID candidateId = securityUtils.getCurrentUserId();
        Boolean res = candidateCertificationsService.getHasCertification(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Has certification fetched successfully"));
    }

}
