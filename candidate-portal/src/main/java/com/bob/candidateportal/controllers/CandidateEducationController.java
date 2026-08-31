package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CandidateEducationService;
import com.bob.commonutil.model.EducationResponseModel;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.EducationDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/education")
public class CandidateEducationController {

    @Autowired
    private CandidateEducationService candidateEducationService;
    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping(value = "/save-edu-details/{docCode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> saveCandidateEducation(@PathVariable String docCode,
                                                                 @RequestPart(value = "file", required = false) MultipartFile file,
                                                                 @Valid @RequestPart EducationDTO education) throws IOException {

        UUID candidateId = securityUtils.getCurrentUserId();
        EducationResponseModel res = candidateEducationService.saveCandidateEducation(candidateId,education,file, docCode);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate education details saved successfully"));
    }

    @GetMapping("/get-edu-details")
    public ResponseEntity<ApiResponse<?>> getCandidateEducationDetails(){
        UUID candidateId = securityUtils.getCurrentUserId();
        List<EducationResponseModel> res = candidateEducationService.getCandidateEducationDetails(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate education details fetched successfully"));
    }

    @DeleteMapping("delete-edu-details")
    public ResponseEntity<ApiResponse<?>> deleteCandidateEducationDetails(@RequestParam UUID educationId){
        candidateEducationService.deleteCandidateEducationDetails( educationId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Candidate education details deleted successfully"));
    }
}
