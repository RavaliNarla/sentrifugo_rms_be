package com.bob.jobportal.controller;

import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.ApiResponse;
import com.bob.jobportal.model.CandidateOfferRequestModel;
import com.bob.jobportal.model.SendOfferRequestModel;
import com.bob.jobportal.service.CandidateOfferService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/candidate-offer")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class CandidateOfferController {

    @Autowired
    private CandidateOfferService candidateOfferService;

    @PostMapping("/send-to-offer-pool")
    public ResponseEntity<ApiResponse<?>> sendToOfferPool(@RequestBody List<UUID> interviews){
        candidateOfferService.sendToOfferPool(interviews);
        return ResponseEntity.ok(ApiResponse.ok("Applications sent to offer pool successfully"));
    }

    @GetMapping("/get-offers/{positionId}")
    public ResponseEntity<ApiResponse<List<CandidateOfferRequestModel>>> getOffersByPositionId(@PathVariable UUID positionId) {
        List<CandidateOfferRequestModel> offers = candidateOfferService.getOffersByPositionId(positionId);
        return ResponseEntity.ok(ApiResponse.ok(offers, "Candidate offers fetched successfully"));
    }

    @PostMapping("/send-offer")
    public ResponseEntity<ApiResponse<?>> sendOffer(@RequestBody SendOfferRequestModel request) throws IOException {
        candidateOfferService.sendOffer(request);
        return ResponseEntity.ok(ApiResponse.ok("Offers sent successfully"));
    }

    @PostMapping("/send-offer/for-approval")
    public ResponseEntity<ApiResponse<?>> sendOfferToApproval(@RequestBody SendOfferRequestModel requestModel){
        candidateOfferService.sendOfferToApproval(requestModel);
        return ResponseEntity.ok(ApiResponse.ok("Offers sent for Approval successfully!"));
    }

    @PostMapping("/download-offers-excel")
    public ResponseEntity<byte[]> downloadOffersExcel(@RequestBody List<UUID> offerIds) throws IOException {
        byte[] excelBytes = candidateOfferService.downloadOffersExcel(offerIds);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=candidate_offers.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @Operation(summary = "Upload offers Excel to update select list and wait list")
    @PostMapping(value = "/upload-ranks-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadOffersExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }
        candidateOfferService.uploadOffersExcel(file);
        return ResponseEntity.ok(ApiResponse.ok("Offer data updated successfully"));
    }

    @GetMapping("/download-rank-list/{positionId}")
    public ResponseEntity<byte[]> downloadRankList(@PathVariable UUID positionId) throws IOException {
        byte[] excelBytes = candidateOfferService. downloadMeritListExcel(positionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=candidate_merit_list.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/download-assign-locations-excel/{positionId}")
    public ResponseEntity<byte[]> downloadAssignLocationExcel(@PathVariable UUID positionId) throws IOException {
        byte[] excelBytes = candidateOfferService.downloadAssignLocationExcel(positionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=candidate_merit_list.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @PostMapping(value = "/upload-assign-locations-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadLocationsExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }
        candidateOfferService.uploadOffersExcelNew(file);
        return ResponseEntity.ok(ApiResponse.ok("Offer data updated successfully"));
    }


    @PostMapping("/generate-offers")
    public ResponseEntity<ApiResponse<Object>> generateOffers(@RequestBody SendOfferRequestModel sendOfferRequestModel){
        candidateOfferService.generateOffers(sendOfferRequestModel);
        return ResponseEntity.ok(ApiResponse.ok("Offer Letter generated successfully!"));
    }


    @PostMapping(value = "/download-offers/zip", produces = "application/zip")
    public void downloadOfferLettersAsZip(@RequestBody List<UUID> offerIds, HttpServletResponse response) {
        // 1. Set the headers to trigger a file download in the browser
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Disposition", "attachment; filename=\"candidate_offers.zip\"");
        try {
            // 2. Pass the output stream to the service so it streams on the fly
            candidateOfferService.streamOffersAsZip(offerIds, response.getOutputStream());
            // Note: We do flushBuffer here to ensure all bytes are sent
            response.flushBuffer();
        } catch (IOException e) {
            // If the stream fails, reset the response and send an error status
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/upload-signed-offers/{positionId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Object>> uploadSignedOfferLetters(@PathVariable UUID positionId,@RequestParam("file") MultipartFile zipFile) {
        if (zipFile.isEmpty() || !zipFile.getOriginalFilename().endsWith(".zip")) {
            throw new IllegalArgumentException("Please upload a valid ZIP file.");
        }
        Map<String,Object> finalRes=candidateOfferService.processSignedOffersZip(positionId,zipFile);

        return ResponseEntity.ok(ApiResponse.ok(finalRes,"Signed Offers Uploaded Successfully!"));
    }

}
