package com.bob.masterdata.Controller;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.DepartmentsDTO;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.masterdata.Service.DepartmentsService;
import com.bob.commonutil.service.ExcelTemplateService;
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
@RequestMapping("${master.api.base.path}/departments")
public class DepartmentsController {
    @Autowired
    private DepartmentsService departmentsService;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<DepartmentsDTO>>> getAllCities(){
            List<DepartmentsDTO> departments = departmentsService.getAllDepartments();
            ApiResponse<List<DepartmentsDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", departments);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentsDTO>> createdepartments(@Valid @RequestBody DepartmentsDTO departments){

            DepartmentsDTO msg = departmentsService.createDepartment(departments);
            ApiResponse<DepartmentsDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentsDTO>> updatedepartments(@PathVariable UUID id,@Valid @RequestBody DepartmentsDTO departments){
            DepartmentsDTO msg = departmentsService.updateDepartments(id, departments);
            ApiResponse<DepartmentsDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentsDTO>> deletedepartments(@PathVariable UUID id){
            DepartmentsDTO departments = departmentsService.deleteDepartments(id);
            ApiResponse<DepartmentsDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", departments);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {
            ExcelTemplateFile excelTemplateFile = excelTemplateService.generateExcelTemplate(DepartmentsDTO.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(AppConstants.CONTENT_DISPOSITION_ATTACHMENT, excelTemplateFile.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelTemplateFile.getFileContent());

    }

    @Operation(summary = "Bulk add departments from an Excel file")
    @PostMapping(value = "/bulk-add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentsDTO>>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }

        List<DepartmentsDTO> departmentsDTOS=departmentsService.bulkSave(file);


        ApiResponse<List<DepartmentsDTO>> response =
                new ApiResponse<>(
                        true,
                        "Uploaded the file successfully: " + file.getOriginalFilename(),
                        departmentsDTOS
                );

        return ResponseEntity.ok(response);
    }


}
