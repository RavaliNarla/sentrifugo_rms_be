package com.bob.masterdata.Controller;


import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.SpecialCategoriesDTO;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.masterdata.Service.SpecialCategoriesService;
import com.bob.commonutil.exception.ExcelValidationException;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("${master.api.base.path}/special-categories")
public class SpecialCategoriesController {

    @Autowired
    private SpecialCategoriesService specialCategoriesService;

    @Autowired
    private ExcelTemplateService excelTemplateService;


    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SpecialCategoriesDTO>>> getAllSpecialCategories()  {
            List<SpecialCategoriesDTO> specialCategories = specialCategoriesService.getAllSpecialCategories();
            ApiResponse<List<SpecialCategoriesDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", specialCategories);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SpecialCategoriesDTO>> createSpecialCategories(@RequestBody SpecialCategoriesDTO specialCategoriesDto){

            SpecialCategoriesDTO msg = specialCategoriesService.createSpecialCategory(specialCategoriesDto);
            ApiResponse<SpecialCategoriesDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SpecialCategoriesDTO>> updateSpecialCategories(@PathVariable UUID id, @RequestBody SpecialCategoriesDTO specialCategoriesDto){
            SpecialCategoriesDTO msg = specialCategoriesService.updateSpecialCategory(id, specialCategoriesDto);
            ApiResponse<SpecialCategoriesDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SpecialCategoriesDTO>> deleteSpecialCategories(@PathVariable UUID id){
            SpecialCategoriesDTO specialCategoriesEntity = specialCategoriesService.deleteCategory(id);
            ApiResponse<SpecialCategoriesDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", specialCategoriesEntity);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @GetMapping("/download-template")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {
            ExcelTemplateFile excelTemplateFile = excelTemplateService.generateExcelTemplate(SpecialCategoriesDTO.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(AppConstants.CONTENT_DISPOSITION_ATTACHMENT, excelTemplateFile.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelTemplateFile.getFileContent());
    }

    @Operation(summary = "Bulk add Special Categories from an Excel file")
    @PostMapping(value = "/bulk-add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<SpecialCategoriesDTO>>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }

        List<SpecialCategoriesDTO> specialCategories =
                specialCategoriesService.bulkSave(file);

        ApiResponse<List<SpecialCategoriesDTO>> response =
                new ApiResponse<>(
                        true,
                        "Uploaded the file successfully: " + file.getOriginalFilename(),
                        specialCategories
                );

        return ResponseEntity.ok(response);
    }

}
