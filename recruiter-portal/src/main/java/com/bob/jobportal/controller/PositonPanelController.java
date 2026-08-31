package com.bob.jobportal.controller;

import com.bob.commonutil.exception.CommonException;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.InterviewPanelsDTO;
import com.bob.db.dto.PositionPanelDTO;
import com.bob.jobportal.model.PositionPanelResponseModel;
import com.bob.jobportal.service.PositionPanelService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/position-panel")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class PositonPanelController {

    @Autowired
    private PositionPanelService positionPanelService;


    @GetMapping("/get-by-position-id/{positionId}")
    public ResponseEntity<ApiResponse<PositionPanelResponseModel>> getPositionPanelsByPositionId(@PathVariable UUID positionId){
        PositionPanelResponseModel res = positionPanelService.getGroupedPositionPanelsByPositionId(positionId);
        ApiResponse<PositionPanelResponseModel> response = ApiResponse.ok(res,"Fetched Position Panels by Position ID successfully");
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @PostMapping("/save-or-update/{jobPositionId}")
    public ResponseEntity<ApiResponse<PositionPanelResponseModel>> saveOrUpdatePositionPanel(
            @PathVariable UUID jobPositionId,
            @Valid @RequestBody PositionPanelResponseModel positionPanelResponseModel) throws MessagingException, IOException {

        Boolean saved = positionPanelService.saveOrUpdatePositionPanel(positionPanelResponseModel, jobPositionId);
        if(saved){
            PositionPanelResponseModel savedModel = positionPanelService.getGroupedPositionPanelsByPositionId(jobPositionId);
            ApiResponse<PositionPanelResponseModel> response = ApiResponse.ok(savedModel, "Position panel saved/updated successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            ApiResponse<PositionPanelResponseModel> response = ApiResponse.error("Failed to save or update position panel!");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private record CommitteeRequest(List<UUID> positionPanelIds, String comments){}

    @PostMapping("/approve-committee")
    public ResponseEntity<ApiResponse<PositionPanelResponseModel>> approveCommittee(@RequestBody CommitteeRequest request) throws MessagingException, IOException {
            positionPanelService.approveCommittee(request.positionPanelIds,request.comments);
            ApiResponse<PositionPanelResponseModel> response = ApiResponse.ok(null,"Position panel list approved successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/reject-committee")
    public ResponseEntity<ApiResponse<PositionPanelResponseModel>> rejectCommittee(@RequestBody CommitteeRequest request) {
            positionPanelService.rejectCommittee(request.positionPanelIds,request.comments);
            ApiResponse<PositionPanelResponseModel> response = ApiResponse.ok(null,"Position panel list rejected successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/download-assignment-template")
    public ResponseEntity<byte[]> downloadPanelTemplate() {

        try (Workbook workbook = positionPanelService.generatePanelAssignmentExcelTemplate();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            workbook.write(outputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
            );

            headers.setContentDisposition(
                    ContentDisposition
                            .attachment()
                            .filename("Panel_Assignment.xlsx")
                            .build()
            );

            return new ResponseEntity<>(
                    outputStream.toByteArray(),
                    headers,
                    HttpStatus.OK
            );

        } catch (Exception e) {
            throw new CommonException("Failed to generate Excel template");
        }
    }

    @Operation(summary = "Assign Bulk panels from an Excel file")
    @PostMapping(path = "/upload-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<PositionPanelDTO>>> uploadExcelTemplate(@RequestParam("file") MultipartFile excelFile) throws IOException, MessagingException {
        List<PositionPanelDTO> positionPanelDTOS=positionPanelService.uploadPanelAssignments(excelFile);
        return ResponseEntity.ok(ApiResponse.ok(positionPanelDTOS,"Panel assigned to positions successfully."));
    }

}
