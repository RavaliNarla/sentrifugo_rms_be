package com.bob.jobportal.controller;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.model.DashBoardInputModel;
import com.bob.jobportal.service.DashboardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${recruiter.api.base.path}/dashboard")
public class DashBoardController {

    @Autowired
    private DashboardService dashboardService;

     // Procedure: gold.fn_dashboard_filters()
     @GetMapping("/filters")
     public ResponseEntity<ApiResponse<JsonNode>> getDashboardFilters() throws JsonProcessingException {
        JsonNode result = dashboardService.getDashboardFilters();
        ApiResponse<JsonNode> response = ApiResponse.ok(result, "Dashboard filters fetched successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

     //Procedure: gold.fn_dashboard_data
    @PostMapping("/details")
    public ResponseEntity<ApiResponse<JsonNode>> detailTotalVacancies(@RequestBody DashBoardInputModel filter) {
        JsonNode result = dashboardService.getDashboardDetails(filter);
        ApiResponse<JsonNode> response = ApiResponse.ok(result, "Dashboard details fetched successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> getAllDetails(@RequestBody DashBoardInputModel filter){

        byte[] fileBytes = dashboardService.getDashboardReport(filter);
        MediaType fileType = AppConstants.DOWNLOAD_DOC_TYPE_XLSX.equals(filter.getExtension()) ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                :MediaType.APPLICATION_PDF;
        String filename = filter.getReportScreen().toString() + filter.getExtension();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(fileType);

        headers.set(HttpHeaders.CONTENT_DISPOSITION, AppConstants.CONTENT_DISPOSITION_ATTACHMENT + "; filename=\"" + filename + "\"");


        return ResponseEntity.ok()
                .headers(headers) // Passes in both the content-type and content-disposition
                .body(fileBytes);

    }

}
