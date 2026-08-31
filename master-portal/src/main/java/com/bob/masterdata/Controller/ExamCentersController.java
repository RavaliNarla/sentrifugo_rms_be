package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.ExamCenterDTO;
import com.bob.masterdata.Service.ExamCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${master.api.base.path}/written-exam-centers")
public class ExamCentersController {

    @Autowired
    private ExamCenterService examCenterService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ExamCenterDTO>>> getAllExamCenters() {
       return ResponseEntity.ok(ApiResponse.ok(examCenterService.getAllExamCenters(), "Exam centers fetched successfully"));
    }
}
