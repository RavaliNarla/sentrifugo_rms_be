package com.bob.jobportal.controller;

import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.JobPositionsDTO;
import com.bob.jobportal.model.JobPositionResponseModel;
import com.bob.jobportal.model.PositionVacancyBreakdownModel;
import com.bob.jobportal.service.JobPositionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/job-positions")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class JobPositionsController {
    @Autowired
    JobPositionsService jobPositionsService;

    @Operation(summary = "Create Job Position")
    @RequestBody(
            content = @Content(
                    encoding = @Encoding(name = "jobPositionsDTO", contentType = "application/json")
            )
    )
    @PostMapping(value = "/create-job-position", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<JobPositionsDTO>> createJobPosition(
            @RequestPart(value = "indentFile", required = false) MultipartFile indentFile,
            @Valid @RequestPart(value = "jobPositionsDTO") JobPositionsDTO jobPositionsDTO){

        JobPositionsDTO created = jobPositionsService.createJobPosition(indentFile, jobPositionsDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created, "Job Position Created Successfully"));
    }

    @PostMapping(value="/update-job-position",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<JobPositionsDTO>> updateJobPosition(
            @RequestPart(value = "indentFile", required = false) MultipartFile indentFile,
            @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @Valid @RequestPart(value = "jobPositionsDTO") JobPositionsDTO jobPositionsDTO){
        JobPositionsDTO updated = jobPositionsService.updateJobPosition(indentFile,jobPositionsDTO);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Job Position Updated Successfully"));
    }

    @GetMapping("/get-job-position-by-requisition/{requisitionId}")
    public ResponseEntity<ApiResponse<List<JobPositionsDTO>>> getPositionsByRequisitionId(@PathVariable UUID requisitionId) {
        List<JobPositionsDTO> positions = jobPositionsService.getPositionsByRequisitionId(requisitionId);
        return ResponseEntity.ok(ApiResponse.ok(positions,AppConstants.JOB_POSITIONS_FETCHED_MESSAGE));
    }

    @GetMapping("/get-draft-job-position-by-requisition/{requisitionId}")
    public ResponseEntity<ApiResponse<List<JobPositionsDTO>>> getDraftPositionsByRequisitionId(@PathVariable UUID requisitionId) {
        List<JobPositionsDTO> positions = jobPositionsService.getDraftPositionsByRequisitionId(requisitionId);
        return ResponseEntity.ok(ApiResponse.ok(positions, AppConstants.JOB_POSITIONS_FETCHED_MESSAGE));
    }

    @GetMapping("/get-job-position-by-id/{positionId}")
    public ResponseEntity<ApiResponse<JobPositionsDTO>> getPositionById(@PathVariable UUID positionId) {
        JobPositionsDTO positions = jobPositionsService.getPositionById(positionId);
        return ResponseEntity.ok(ApiResponse.ok(positions,AppConstants.JOB_POSITIONS_FETCHED_MESSAGE));
    }

    @DeleteMapping("/delete-job-position-by-id/{positionId}")
    public ResponseEntity<ApiResponse<Void>> deletePositionById(@PathVariable UUID positionId) {
        jobPositionsService.deletePositionById(positionId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Job Position Deleted Successfully"));
    }

    @GetMapping("/download-template")
    public ResponseEntity<byte[]> download() {
            byte[] excelTemplate = jobPositionsService.generateJobPositionExcelTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(AppConstants.CONTENT_DISPOSITION_ATTACHMENT, "JobPositionsTemplate.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelTemplate);
    }

    @Operation(summary = "Bulk add positions from an Excel file")
    @PostMapping(value = "/create-bulk-positions/{requisitionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<JobPositionsDTO>>> createBulkPositions(
            @PathVariable UUID requisitionId,
            @RequestParam("file") MultipartFile file) {

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }

        List<JobPositionsDTO> positions = jobPositionsService.bulkSaveFromExcel(requisitionId, file);

        ApiResponse<List<JobPositionsDTO>> response =
                new ApiResponse<>(
                        true,
                        "Uploaded the file successfully: " + file.getOriginalFilename(),
                        positions
                );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/get-positions")
    public ResponseEntity<ApiResponse<List<JobPositionResponseModel>>> getActivePositionsByRequisition(@RequestParam(required = true) UUID requisitionId, @RequestParam(required = false) String searchText) {
        List<JobPositionResponseModel> positions = jobPositionsService.getActivePosByReqId(requisitionId,searchText);
        return ResponseEntity.ok(ApiResponse.ok(positions,AppConstants.JOB_POSITIONS_FETCHED_MESSAGE));
    }

    @GetMapping("/vacancy-breakdown/by-requisition/{requisitionId}")
    public ResponseEntity<ApiResponse<List<PositionVacancyBreakdownModel>>> getVacancyBreakdownByRequisition(
            @PathVariable UUID requisitionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                jobPositionsService.getVacancyBreakdownByRequisition(requisitionId),
                "Vacancy breakdown fetched successfully"));
    }

    @GetMapping("/vacancy-breakdown/by-position/{positionId}")
    public ResponseEntity<ApiResponse<PositionVacancyBreakdownModel>> getVacancyBreakdownByPosition(
            @PathVariable UUID positionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                jobPositionsService.getVacancyBreakdownByPosition(positionId),
                "Vacancy breakdown fetched successfully"));
    }
}
