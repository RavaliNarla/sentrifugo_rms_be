package com.bob.jobportal.controller;

import com.bob.db.dto.ApiResponse;
import com.bob.jobportal.model.ExaminationMarksSummaryModel;
import com.bob.jobportal.service.CandidateWrittenExamMarksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/examination-marks")
public class CandidateWrittenExamMarksController{

    @Autowired
    private CandidateWrittenExamMarksService candidateWrittenExamMarksService;

    @PostMapping("/download-template")
    public ResponseEntity<byte[]> download(@RequestBody List<UUID> positionIds) {
        byte[] excelTemplate = candidateWrittenExamMarksService.downloadMarkUploadTemplate(positionIds);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition
                        .attachment()
                        .filename("ExamConfigurationMarksUpload.xlsx")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelTemplate);
    }

    @PostMapping(value = "/upload-marks",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadExcel(@RequestBody MultipartFile file){
        candidateWrittenExamMarksService.saveExaminationMarks(file);
        return new ResponseEntity<>(ApiResponse.ok(null,"Examination marks saved successfully"), HttpStatus.OK);

    }

    @PostMapping("/get-summary")
    public ResponseEntity<ApiResponse<List<ExaminationMarksSummaryModel>>> getOverallSummary(@RequestBody List<UUID> positionIds){
        List<ExaminationMarksSummaryModel> summaryModel = candidateWrittenExamMarksService.getOverallMarksSummary(positionIds);
        ApiResponse<List<ExaminationMarksSummaryModel>> response = ApiResponse.ok(summaryModel,"Exam summary fetched successfully");
        return new ResponseEntity<>(response,HttpStatus.OK);
    }
}
