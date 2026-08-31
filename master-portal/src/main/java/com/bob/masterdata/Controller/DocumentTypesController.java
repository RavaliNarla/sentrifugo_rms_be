package com.bob.masterdata.Controller;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.DocumentTypesDTO;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.masterdata.Service.DocumentTypesService;
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
@RequestMapping("${master.api.base.path}/document-types")
public class DocumentTypesController {

    @Autowired
    private DocumentTypesService documentTypesService;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentTypesDTO>> addDocumentType(@Valid @RequestBody DocumentTypesDTO documentType) {
        DocumentTypesDTO createdDocumentType = documentTypesService.addDocumentType(documentType);
        ApiResponse<DocumentTypesDTO> response = new ApiResponse<>(true, "Document type added successfully", createdDocumentType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllDocumentTypes() {
        List<DocumentTypesDTO> documentTypes = documentTypesService.getAllDocumentTypes();
        ApiResponse<?> response = new ApiResponse<>(true, "Fetched all document types successfully", documentTypes);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentTypesDTO>> updateDocumentType(@PathVariable UUID id,@Valid @RequestBody DocumentTypesDTO documentType) {
        DocumentTypesDTO updatedDocumentType = documentTypesService.updateDocumentType(id, documentType);
        ApiResponse<DocumentTypesDTO> response = new ApiResponse<>(true, "Document type updated successfully", updatedDocumentType);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentTypesDTO>> deleteDocumentType(@PathVariable UUID id) {
        DocumentTypesDTO deletedDocumentType = documentTypesService.deleteDocumentType(id);
        ApiResponse<DocumentTypesDTO> response = new ApiResponse<>(true, "Document type deleted successfully", deletedDocumentType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{docType}")
    public ResponseEntity<ApiResponse<?>> getDocumentTypesByType(@PathVariable String docType) {
        List<DocumentTypesDTO> documentTypes = documentTypesService.getDocumentTypesByType(docType);
        ApiResponse<?> response = new ApiResponse<>(true, "Fetched document types by type successfully", documentTypes);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {
            ExcelTemplateFile excelTemplateFile = excelTemplateService.generateExcelTemplate(DocumentTypesDTO.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(AppConstants.CONTENT_DISPOSITION_ATTACHMENT, excelTemplateFile.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelTemplateFile.getFileContent());

    }
    @Operation(summary = "Bulk add document types from an Excel file")
    @PostMapping(value = "/bulk-add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentTypesDTO>>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }

        List<DocumentTypesDTO> documentTypes =
                documentTypesService.bulkSave(file);

        ApiResponse<List<DocumentTypesDTO>> response =
                new ApiResponse<>(
                        true,
                        "Uploaded the file successfully: " + file.getOriginalFilename(),
                        documentTypes
                );

        return ResponseEntity.ok(response);
    }

}
