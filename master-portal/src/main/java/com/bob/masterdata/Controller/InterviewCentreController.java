package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.InterviewCentresDTO;

import com.bob.masterdata.Model.InterviewCenterRequestModel;
import com.bob.masterdata.Service.InterviewCentresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/interview-centres")
public class InterviewCentreController {

    @Autowired
    private InterviewCentresService interviewCentresService;

    @PostMapping ("/search")
    public ResponseEntity<ApiResponse<List<InterviewCentresDTO>>> searchInterviewCentresByFilter(@RequestBody InterviewCenterRequestModel model) {
        List<InterviewCentresDTO> interviewCentresDTOS = interviewCentresService.searchInterviewCentresByFilter(model);
        ApiResponse<List<InterviewCentresDTO>> response = ApiResponse.ok(interviewCentresDTOS, "INTERVIEW CENTRES FETCHED SUCCESSFULLY!");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
