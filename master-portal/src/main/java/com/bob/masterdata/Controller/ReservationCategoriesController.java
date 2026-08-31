package com.bob.masterdata.Controller;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.ReservationCategoriesDTO;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.enums.ReservationType;
import com.bob.masterdata.Service.ReservationCategoriesService;
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
@RequestMapping("${master.api.base.path}/reservation-categories")
public class ReservationCategoriesController {
    @Autowired
    private ReservationCategoriesService reservationCategoriesService;


    @Autowired
    private ExcelTemplateService excelTemplateService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ReservationCategoriesDTO>>> getAllReservations(@RequestParam(required = false) ReservationType reservationType){
            List<ReservationCategoriesDTO> resCategories = reservationCategoriesService.getAllResCategories(reservationType);
            ApiResponse<List<ReservationCategoriesDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", resCategories);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ReservationCategoriesDTO>> createReservation(@RequestBody ReservationCategoriesDTO reservationCategoriesDto){

            ReservationCategoriesDTO reservationCategoriesEntity1 = reservationCategoriesService.createReservationCategories(reservationCategoriesDto);
            ApiResponse<ReservationCategoriesDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", reservationCategoriesEntity1);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ReservationCategoriesDTO>> updateReservations(@PathVariable UUID id, @RequestBody ReservationCategoriesDTO reservationCategoriesDto){
            ReservationCategoriesDTO reservationCategoriesEntity1 = reservationCategoriesService.updateResCategories(id, reservationCategoriesDto);
            ApiResponse<ReservationCategoriesDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", reservationCategoriesEntity1);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ReservationCategoriesDTO>> deleteReservations(@PathVariable UUID id){
            ReservationCategoriesDTO reservationCategoriesEntity = reservationCategoriesService.deleteReservationCategory(id);
            ApiResponse<ReservationCategoriesDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", reservationCategoriesEntity);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @GetMapping("/download-template")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {
            ExcelTemplateFile excelTemplateFile = excelTemplateService.generateExcelTemplate(ReservationCategoriesDTO.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(AppConstants.CONTENT_DISPOSITION_ATTACHMENT, excelTemplateFile.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelTemplateFile.getFileContent());
    }

    @Operation(summary = "Bulk add reservation categories from an Excel file")
    @PostMapping(value = "/bulk-add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReservationCategoriesDTO>>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }

        List<ReservationCategoriesDTO> categories =
                reservationCategoriesService.bulkSave(file);

        ApiResponse<List<ReservationCategoriesDTO>> response =
                new ApiResponse<>(
                        true,
                        "Uploaded the file successfully: " + file.getOriginalFilename(),
                        categories
                );

        return ResponseEntity.ok(response);
    }


}
