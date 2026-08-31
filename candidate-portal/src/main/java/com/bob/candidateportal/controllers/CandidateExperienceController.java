package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CandidateExperienceService;
import com.bob.commonutil.model.WorkExperienceResponseModel;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.WorkExperienceDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/experience")
public class CandidateExperienceController {

    @Autowired
    private CandidateExperienceService candidateExperienceService;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping(value = "/save-exp-details",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WorkExperienceResponseModel>> saveCandidateExperience(
                                                                                  @RequestPart(value = "file", required = false) MultipartFile file,
                                                                                  @Valid @RequestPart WorkExperienceDTO workExperience

    ){
        UUID candidateId = securityUtils.getCurrentUserId();
        WorkExperienceResponseModel res = candidateExperienceService.saveExperienceDetails(candidateId, workExperience, file);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate experience details saved successfully"));
    }

    @GetMapping("/get-exp-details")
    public ResponseEntity<ApiResponse<?>> getCandidateExperienceDetails(){
        UUID candidateId = securityUtils.getCurrentUserId();
        List<WorkExperienceResponseModel> res = candidateExperienceService.getCandidateExperienceDetails(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate experience details fetched successfully"));
    }

    @DeleteMapping("/delete-exp/{workExperienceId}")
    public ResponseEntity<ApiResponse<?>> deleteCandidateExperience(@PathVariable UUID workExperienceId) {
        candidateExperienceService.deleteCandidateExperience(workExperienceId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Candidate experience details deleted successfully"));
    }


    @PostMapping(value = "/save-all-exp-details")
    public ResponseEntity<ApiResponse<List<WorkExperienceDTO>>> saveAllCandidateExperience(@Valid @RequestBody List<@Valid WorkExperienceDTO> workExperienceList) {
        UUID candidateId = securityUtils.getCurrentUserId();
        List<WorkExperienceDTO> res = candidateExperienceService.saveAllExperienceDetails(candidateId, workExperienceList);
        return ResponseEntity.ok(ApiResponse.ok(res, "Candidate experience details saved successfully"));
    }

}
