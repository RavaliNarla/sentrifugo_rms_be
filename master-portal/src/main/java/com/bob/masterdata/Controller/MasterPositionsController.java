package com.bob.masterdata.Controller;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.MasterPositionsDTO;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.masterdata.Model.MasterPositionExcelModel;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.masterdata.Service.MasterPositionsService;
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
@RequestMapping("${master.api.base.path}/master-positions")
public class MasterPositionsController {

    @Autowired
    private MasterPositionsService masterPositionsService;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<MasterPositionsDTO>> create(@RequestBody MasterPositionsDTO dto) {
        MasterPositionsDTO created = masterPositionsService.create(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Created successfully", created));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<MasterPositionsDTO>> update(@PathVariable UUID id, @RequestBody MasterPositionsDTO dto) {
        MasterPositionsDTO updated = masterPositionsService.update(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Updated successfully", updated));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<MasterPositionsDTO>>> getAll() {
        List<MasterPositionsDTO> list = masterPositionsService.getAll();
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", list));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        masterPositionsService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Deleted successfully", null));
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {
            ExcelTemplateFile excelTemplateFile = excelTemplateService.generateExcelTemplate(MasterPositionExcelModel.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(AppConstants.CONTENT_DISPOSITION_ATTACHMENT, excelTemplateFile.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelTemplateFile.getFileContent());

    }

    @Operation(summary = "Bulk add master positions from an Excel file")
    @PostMapping(value = "/bulk-add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<MasterPositionsDTO>>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }

        List<MasterPositionsDTO> masterPositions =
                masterPositionsService.bulkSave(file);

        ApiResponse<List<MasterPositionsDTO>> response =
                new ApiResponse<>(
                        true,
                        "Uploaded the file successfully: " + file.getOriginalFilename(),
                        masterPositions
                );

        return ResponseEntity.ok(response);
    }

}
