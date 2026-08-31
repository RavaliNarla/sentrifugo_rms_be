package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.SkillDTO;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.masterdata.Service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/skill")
public class SkillController {
    @Autowired
    private SkillService skillService;

    @Autowired
    private ExcelTemplateService excelTemplateService;
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SkillDTO>>> getAllSkills(){
            List<SkillDTO> skills = skillService.getAllSkills();
            ApiResponse<List<SkillDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", skills);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SkillDTO>> createSkill(@RequestBody SkillDTO skillDto){

            SkillDTO msg = skillService.createSkill(skillDto);
            ApiResponse<SkillDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SkillDTO>> updateSkill(@PathVariable UUID id, @RequestBody SkillDTO skillDto){
            SkillDTO msg = skillService.updateSKill(id, skillDto);
            ApiResponse<SkillDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SkillDTO>> deleteSkill(@PathVariable UUID id){
            SkillDTO skillDto = skillService.deleteSkill(id);
            ApiResponse<SkillDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", skillDto);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
