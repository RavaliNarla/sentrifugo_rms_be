package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.masterdata.Model.AdminEducationModel;
import com.bob.masterdata.Service.AdminEducationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/admin-education-master")
public class AdminEducationController {


        @Autowired
        private AdminEducationService adminEducationService;

        @PostMapping("/save")
        public ResponseEntity<ApiResponse<Void>> saveEducationDetails(@Valid @RequestBody AdminEducationModel adminEducationModel){
            adminEducationService.saveEducationDetails(adminEducationModel);
            return new ResponseEntity<>(ApiResponse.ok("Education Details Saved/Updated successfully!"), HttpStatus.OK);
        }

        @PostMapping("/all")
        public ResponseEntity<ApiResponse<List<AdminEducationModel>>> getEducationDetails(@RequestBody List<UUID> educationLevelIds){
            List<AdminEducationModel> adminEducationModels = adminEducationService.getAllEducationDetails(educationLevelIds);
            ApiResponse<List<AdminEducationModel>> response = ApiResponse.ok(adminEducationModels,"Fetched Education details successfully");
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
}
