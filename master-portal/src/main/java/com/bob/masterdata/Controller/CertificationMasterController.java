package com.bob.masterdata.Controller;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CertificationMasterDTO;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.masterdata.Service.CertificationMasterService;
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
@RequestMapping("${master.api.base.path}/certificates-master")
public class CertificationMasterController {

    @Autowired
    private CertificationMasterService certificationMasterService;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CertificationMasterDTO>> createCertificate(@Valid @RequestBody CertificationMasterDTO certificationMasterDTO){
        CertificationMasterDTO savedCertificate = certificationMasterService.saveCertificate(certificationMasterDTO);
        ApiResponse<CertificationMasterDTO> savedCertificateResponse = ApiResponse.ok(savedCertificate,"Certificate added successfully");
        return new ResponseEntity<>(savedCertificateResponse, HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CertificationMasterDTO>>> getAllCertificates(){
         List<CertificationMasterDTO> allCertificates = certificationMasterService.getAllCertificates();
         ApiResponse<List<CertificationMasterDTO>> allCertificatesResponse = ApiResponse.ok(allCertificates,"Fetched all certificates successfully");
         return new ResponseEntity<>(allCertificatesResponse,HttpStatus.OK);
    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CertificationMasterDTO>> getCertificateById(@PathVariable UUID id){
        CertificationMasterDTO certificate = certificationMasterService.getCertificateById(id);
        ApiResponse<CertificationMasterDTO> certificateResponse = ApiResponse.ok(certificate,"Certificate fetched successfully");
        return new ResponseEntity<>(certificateResponse,HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CertificationMasterDTO>> updateCertificate( @PathVariable UUID id, @Valid @RequestBody CertificationMasterDTO certificationMasterDTO){
        CertificationMasterDTO   updatedCertificate = certificationMasterService.updateCertificate(id,certificationMasterDTO);
        ApiResponse<CertificationMasterDTO> updatedCertificateResponse = ApiResponse.ok(updatedCertificate,"Certificate updated successfully");
        return new ResponseEntity<>(updatedCertificateResponse,HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCertificate(@PathVariable UUID id){
        certificationMasterService.deleteCertificate(id);
        return  new ResponseEntity<>(new ApiResponse<>(true, "Deleted  certificate successfully", null),HttpStatus.OK);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate(){
            ExcelTemplateFile excelTemplateFile = excelTemplateService.generateExcelTemplate(CertificationMasterDTO.class);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(AppConstants.CONTENT_DISPOSITION_ATTACHMENT, excelTemplateFile.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelTemplateFile.getFileContent());

    }
    @Operation(summary = "Bulk add Certifications from an Excel file")
    @PostMapping(value = "/bulk-add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<CertificationMasterDTO>>> uploadFile(@RequestParam("file") MultipartFile file) {

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }

        List<CertificationMasterDTO> certificationMasterDTOS =
                certificationMasterService.bulkSave(file);

        ApiResponse<List<CertificationMasterDTO>> response =
                new ApiResponse<>(
                        true,
                        "Uploaded the file successfully: " + file.getOriginalFilename(),
                        certificationMasterDTOS
                );

        return ResponseEntity.ok(response);
    }



}
