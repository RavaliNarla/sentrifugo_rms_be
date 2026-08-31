package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.EducationQualificationsDTO;
import com.bob.masterdata.Service.EducationalQualificationsService;
import com.bob.commonutil.service.ExcelTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/eduQual")
public class EducationalQualificationsController {

    @Autowired
    private EducationalQualificationsService educationalQualificationsService;

    @Autowired
    private ExcelTemplateService excelTemplateService;
  
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EducationQualificationsDTO>>> getAllEduQual(){
            List<EducationQualificationsDTO> cities = educationalQualificationsService.getAllEduQual();
            ApiResponse<List<EducationQualificationsDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", cities);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<EducationQualificationsDTO>> createEduQual(@RequestBody EducationQualificationsDTO educationQualificationsDto){

            EducationQualificationsDTO educationQualificationsEntity1 = educationalQualificationsService.createEduQual(educationQualificationsDto);
            ApiResponse<EducationQualificationsDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", educationQualificationsEntity1);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<EducationQualificationsDTO>> updateEduQual(@PathVariable UUID id, @RequestBody EducationQualificationsDTO educationQualificationsEntity){
            EducationQualificationsDTO msg = educationalQualificationsService.updateEduQual(id, educationQualificationsEntity);
            ApiResponse<EducationQualificationsDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<EducationQualificationsDTO>> deleteEduQual(@PathVariable UUID id){
            EducationQualificationsDTO educationQualificationsDto = educationalQualificationsService.deleteEduQual(id);
            ApiResponse<EducationQualificationsDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", educationQualificationsDto);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
