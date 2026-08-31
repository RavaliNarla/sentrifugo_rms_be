package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.ExclusionsMasterDTO;
import com.bob.db.dto.ScoringWeightageDTO;
import com.bob.db.entity.ScoringWeightageEntity;
import com.bob.db.dto.ExservicemanCategoryDTO;
import com.bob.masterdata.Service.MasterDDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${master.api.base.path}/master-dd-data")
public class MasterDDController {

    @Autowired
    private MasterDDService masterDDService;

    @GetMapping("/get/gender")
    public ResponseEntity<ApiResponse<?>> getAllGenders() {
    return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllGenders(), "All genders fetched successfully" )  );
    }
    @GetMapping("/get/maritalstatus")
    public ResponseEntity<ApiResponse<?>> getAllMaritalStatus() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllMaritalStatus(), "All marital statuses fetched successfully" )  );
    }

    @GetMapping("/get/religions")
    public ResponseEntity<ApiResponse<?>> getAllReligions() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllReligions(), "All religions fetched successfully" )  );
    }

    @GetMapping("/get/disabilities")
    public ResponseEntity<ApiResponse<?>> getAllDisabilities() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllDisabilities(), "All disabilities fetched successfully" )  );
    }

    @GetMapping("/get/universities")
    public ResponseEntity<ApiResponse<?>> getAllUniversities() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllUniversities(), "All universities fetched successfully" )  );
    }

    @GetMapping("/get/education-types")
    public ResponseEntity<ApiResponse<?>> getAllEducationTypes() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllEducationTypes(), "All education types fetched successfully" )  );
    }

    @GetMapping("/get/specializations")
    public ResponseEntity<ApiResponse<?>> getAllSpecializations() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllSpecializations(), "All specializations fetched successfully" )  );
    }

    @GetMapping("/get/languages")
    public ResponseEntity<ApiResponse<?>> getAllLanguages() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllLanguages(), "All languages fetched successfully" )  );
    }

    @GetMapping("/get/pincodes")
    public ResponseEntity<ApiResponse<?>> getAllPincodes() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllPincodes(), "All pincodes fetched successfully" )  );
    }
    @GetMapping("/get/committees")
    public ResponseEntity<ApiResponse<?>> getAllCommittees() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllInterviewCommittees(), "All committes fetched successfully" )  );
    }

    @GetMapping("/get/request-types")
    public ResponseEntity<ApiResponse<?>> getAllRequestTypes() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllRequestTypes(), "All request types fetched successfully" )  );
    }

    @GetMapping("/get/interview-centres")
    public ResponseEntity<ApiResponse<?>> getAllInterviewCentres() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllInterviewCentres()
                , "All interview centres fetched successfully" )  );
    }

    @GetMapping("/get/state-languages")
    public ResponseEntity<ApiResponse<?>> getAllStateLanguages(){
        return ResponseEntity.ok(ApiResponse.ok(
                masterDDService.getStateLanguages(),
                "All State Languages fetched successfully"
        ));
    }
    @GetMapping("/get/medical-centres")
    public ResponseEntity<ApiResponse<?>> getAllMedicalCentres() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllMedicalCentres()
                , "All medical centres fetched successfully"));
    }

    @GetMapping("/get/roles")
    public ResponseEntity<ApiResponse<?>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllRoles()
                , "All roles fetched successfully"));
    }
    @GetMapping("/get/weightage-scores")
    public ResponseEntity<ApiResponse<ScoringWeightageDTO>> getAllWeightageScores() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllWeightageScores(), "All weightage scores fetched successfully"));
    }
    @GetMapping("/get/exclusions")
    public ResponseEntity<ApiResponse<List<ExclusionsMasterDTO>>> getAllExclusions() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllExclusions(), "All exclusions fetched successfully"));
    }

    @GetMapping("/get/ex-service-men")
    public ResponseEntity<ApiResponse<List<ExservicemanCategoryDTO>>> getAllExServiceMen() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllExServiceMen(), "All ex-service men fetched successfully"));
    }
    @GetMapping("/get/edu-groups")
    public ResponseEntity<ApiResponse<?>> getAllEducationGroups() {
        return ResponseEntity.ok(ApiResponse.ok(masterDDService.getAllEducationGroups(), "All education groups fetched successfully" )  );
    }
}
