package com.bob.masterdata.Controller;

import com.bob.commonutil.exception.CommonException;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.InterviewPanelsDTO;
import com.bob.db.dto.UserDTO;
import com.bob.masterdata.Service.InterviewPanelsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/interview-panels")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
@Validated
public class InterviewPanelsController {

    @Autowired
    private InterviewPanelsService interviewPanelsService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<InterviewPanelsDTO>> createInterviewPanel(@RequestBody InterviewPanelsDTO interviewPanelsDTO) {
        InterviewPanelsDTO createdPanel = interviewPanelsService.save(interviewPanelsDTO);
        createdPanel = interviewPanelsService.findById(createdPanel.getId());
        ApiResponse<InterviewPanelsDTO> response = new ApiResponse<>(true, "Interview Panel created successfully!", createdPanel);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<ApiResponse<InterviewPanelsDTO>> getInterviewPanelById(@PathVariable UUID id) {
        InterviewPanelsDTO interviewPanelsDTO = interviewPanelsService.findById(id);
        if (interviewPanelsDTO != null) {
            ApiResponse<InterviewPanelsDTO> response = new ApiResponse<>(true, "Interview Panel fetched successfully!", interviewPanelsDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            ApiResponse<InterviewPanelsDTO> response = new ApiResponse<>(false, "Interview Panel not found!", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<InterviewPanelsDTO>>> getAllInterviewPanels() {
        List<InterviewPanelsDTO> interviewPanels = interviewPanelsService.findAll();
        ApiResponse<List<InterviewPanelsDTO>> response = new ApiResponse<>(true, "Interview Panels fetched successfully!", interviewPanels);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity< ApiResponse<Page<InterviewPanelsDTO>>> searchInterviewPanels(
            @RequestParam(required = false) @Size(max = 100) String panelName,
            @RequestParam(required = false) @Size(max = 100) String committeeName,
            @RequestParam(required = false) @Size(max = 100) String panelMemberName,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") int size) {


        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<InterviewPanelsDTO> result = interviewPanelsService.searchInterviewPanels(
                panelName, committeeName, panelMemberName, pageable);
        ApiResponse<Page<InterviewPanelsDTO>> response = new ApiResponse<>(true, "Interview Panels searched successfully!", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<InterviewPanelsDTO>> updateInterviewPanel(@PathVariable UUID id, @RequestBody InterviewPanelsDTO interviewPanelsDTO) throws MessagingException, IOException {
        InterviewPanelsDTO updatedPanel = interviewPanelsService.update(id, interviewPanelsDTO);
        if (updatedPanel != null) {
            updatedPanel= interviewPanelsService.findById(updatedPanel.getId());
            ApiResponse<InterviewPanelsDTO> response = new ApiResponse<>(true, "Interview Panel updated successfully!", updatedPanel);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            ApiResponse<InterviewPanelsDTO> response = new ApiResponse<>(false, "Interview Panel not found for update!", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInterviewPanel(@PathVariable UUID id) {
        boolean deleted = interviewPanelsService.delete(id);
        if (deleted) {
            ApiResponse<Void> response = new ApiResponse<>(true, "Interview Panel deleted successfully!", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            ApiResponse<Void> response = new ApiResponse<>(false, "Interview Panel not found for deletion!", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }
    @GetMapping("/get/panel-members")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getPanelMembers(){
        List<UserDTO> panelMembers = interviewPanelsService.getPanelMembers();
        ApiResponse<List<UserDTO>> response = new ApiResponse<>(true, "Panel members fetched successfully!", panelMembers);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/check-schedule/{id}")
    public ResponseEntity<ApiResponse<Boolean>> checkWhetherScheduledOrNot(@PathVariable UUID id){
        boolean isScheduled = interviewPanelsService.findWhetherScheduledOrNot(id);
        ApiResponse<Boolean> response = new ApiResponse<>(true, "Panel schedule status fetched successfully!", isScheduled);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/download-panel-template")
    public ResponseEntity<byte[]> downloadPanelTemplate() {

        try (Workbook workbook = interviewPanelsService.generatePanelExcelTemplate();
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
                            .filename("Panel_Creation.xlsx")
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
    @Operation(summary = "Bulk add Panels from an Excel file")
    @PostMapping(path = "/upload-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<InterviewPanelsDTO>>> uploadExcelTemplate(@RequestParam("file") MultipartFile excelFile) {
        List<InterviewPanelsDTO> interviewPanelsDTOS=interviewPanelsService.uploadExcel(excelFile);
        return ResponseEntity.ok(ApiResponse.ok(interviewPanelsDTOS,"Bulk Panels Created Successfully!"));
    }
}
