package com.bob.masterdata.Controller;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.JobGradeDTO;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.masterdata.Model.JobGradeExcelModel;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.masterdata.Service.JobGradeService;
import com.bob.commonutil.exception.ExcelValidationException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/jobgrade")
public class JobGradeController {
    @Autowired
    private JobGradeService jobGradeService;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<JobGradeDTO>>> getAllJobGrades(){
            List<JobGradeDTO> jobGrades = jobGradeService.getAllJobGrades();
            ApiResponse<List<JobGradeDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", jobGrades);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<JobGradeDTO>> createJobGrade(@Valid @RequestBody JobGradeDTO jobGradeDto){
            JobGradeDTO jobGradeDto1 = jobGradeService.createJobGrade(jobGradeDto);
            ApiResponse<JobGradeDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", jobGradeDto1);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<JobGradeDTO>> updateJobGrade(@PathVariable UUID id,@Valid @RequestBody JobGradeDTO jobGradeDto){
            JobGradeDTO msg = jobGradeService.updateJobGrade(id, jobGradeDto);
            ApiResponse<JobGradeDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<JobGradeDTO>> deleteJobGrade(@PathVariable UUID id){
            JobGradeDTO jobGradeDto = jobGradeService.deleteJobGrade(id);
            ApiResponse<JobGradeDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", jobGradeDto);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {
            ExcelTemplateFile excelTemplateFile = excelTemplateService.generateExcelTemplate(JobGradeExcelModel.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(AppConstants.CONTENT_DISPOSITION_ATTACHMENT, excelTemplateFile.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelTemplateFile.getFileContent());

    }

    @Operation(summary = "Bulk add Job Grades from an Excel file")
    @PostMapping(value = "/bulk-add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<JobGradeDTO>>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }

        List<JobGradeDTO> jobGrades = jobGradeService.bulkSave(file);

        ApiResponse<List<JobGradeDTO>> response =
                new ApiResponse<>(
                        true,
                        "Uploaded the file successfully: " + file.getOriginalFilename(),
                        jobGrades
                );

        return ResponseEntity.ok(response);
    }

}
