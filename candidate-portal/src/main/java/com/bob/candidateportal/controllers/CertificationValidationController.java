package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CertificationValidationService;
import com.bob.db.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/validate-certification")
public class CertificationValidationController {

    @Autowired
    private CertificationValidationService certificationValidationService;

    @PostMapping(value = "/validate/{certificationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Boolean>> validateCertification(
            @RequestParam("file") MultipartFile file,
            @PathVariable UUID certificationId) {
        try {
            boolean isValid = certificationValidationService.validateCertification(file, certificationId);
            
            if (isValid) {
                return ResponseEntity.ok(
                    ApiResponse.ok(true, "Certification validated successfully")
                );
            } else {
                return ResponseEntity.ok(
                    ApiResponse.ok(false, "Certification validation failed. Required keywords not found in the document")
                );
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Error during validation: " + e.getMessage())
            );
        }
    }
}
