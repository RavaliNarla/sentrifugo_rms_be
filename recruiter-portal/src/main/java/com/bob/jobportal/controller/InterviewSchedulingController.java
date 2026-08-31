package com.bob.jobportal.controller;


import com.azure.core.annotation.Get;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.PositionPanelDTO;
import com.bob.jobportal.model.*;
import com.bob.jobportal.service.InterviewSchedulingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/interview-scheduling")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class InterviewSchedulingController {

    @Autowired
    private InterviewSchedulingService schedulingService;

    @Autowired
    private ExcelTemplateService excelTemplateService;


//    @GetMapping("/download-template")
//    public ResponseEntity<byte[]> downloadTemplate(@RequestBody UUID positionId){
//            ExcelTemplateFile excelTemplateFile=schedulingService.generateExcelTemplate(positionId);
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//            headers.setContentDispositionFormData("attachment", excelTemplateFile.getFileName());
//
//            return ResponseEntity.ok()
//                    .headers(headers)
//                    .body(excelTemplateFile.getFileContent());
//    }

//
//    @PostMapping(
//            value = "/bulk-schedule",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
//    )
//    public ResponseEntity<ApiResponse<?>> scheduleInterview(
//            @RequestPart("file") MultipartFile file,
//            @RequestPart("model") SchedulingPanelModel schedulingPanelModel
//    ) throws IOException {
//        String response=schedulingService.bulkInterviewSchedule(file, schedulingPanelModel);
//        return ResponseEntity.ok(ApiResponse.ok(response));
//    }
//
//    @GetMapping("/resend-failed-emails")
//    public ResponseEntity<ApiResponse<?>> resendFailureMails(){
//        String response=schedulingService.resendFailureMails();
//        return ResponseEntity.ok(ApiResponse.ok(response));
//    }

    @PostMapping("/allocate-interview")
    public ResponseEntity<ApiResponse<?>> allocateInterview(@Valid @RequestBody InterviewSchedulingRequestModel model){
        List<InterviewAllocatedRequestModel> response = schedulingService.allocateInterview(model);
        return ResponseEntity.ok(ApiResponse.ok(response,"Interview Schedulings created successfully"));
    }

    @PostMapping("/schedule-interview")
    public ResponseEntity<ApiResponse<?>> scheduleInterview(@RequestBody SchedulePanelModel schedulePanelModel){
        String response = schedulingService.scheduleInterview(schedulePanelModel);
        return ResponseEntity.ok(ApiResponse.ok(response,"Interview Scheduled successfully"));
    }


    @PostMapping("/get-assigned-panels")
    public ResponseEntity<ApiResponse<?>> getAssignedInterviewPanels(@RequestBody List<UUID> positionIds){
        List<PositionPanelDTO> response = schedulingService.getInterviewPanels(positionIds);
        return ResponseEntity.ok(ApiResponse.ok(response,"Interview panels fetched successfully"));
    }

    @GetMapping("/get-position-panels/{positionId}")
    public ResponseEntity<ApiResponse<?>> getPositionPanels(@RequestParam UUID positionId){
        List<PositionPanelDTO> response = schedulingService.getInterviewPanels(List.of(positionId));
        return ResponseEntity.ok(ApiResponse.ok(response,"Interview panels fetched successfully"));
    }

    @PostMapping("/scheduled-slots")
    public ResponseEntity<ApiResponse<?>> getScheduledSlots(@RequestBody PanelAvailabilityRequestModel panelAvailabilityRequestModel){
        List<PanelAvailabilityModel> response = schedulingService.getScheduledSlots(panelAvailabilityRequestModel);
        return ResponseEntity.ok(ApiResponse.ok(response,"Available slots fetched successfully"));
     }

}
