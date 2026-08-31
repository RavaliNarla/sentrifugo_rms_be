package com.bob.candidateportal.controllers;

import com.bob.commonutil.service.CandidateThreadService;
import com.bob.commonutil.model.candidateportal.RequestHistoryModel;
import com.bob.commonutil.model.candidateportal.CreateThreadRequestModel;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApiResponse;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/candidate-conversation")
public class CandidateConversationController {


    @Autowired
    private CandidateThreadService candidateThreadService;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping(value = "/create-thread",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> createThread(
                                                       @RequestPart(value = "file", required = false) MultipartFile file,
                                                       @RequestPart CreateThreadRequestModel createThreadRequestModel) throws IOException, MessagingException {
//        String role= DBConstants.HEADER_CANDIDATE;
        UUID candidateId = securityUtils.getCurrentUserId();
        RequestHistoryModel submittedRequest = candidateThreadService.createThread(candidateId,createThreadRequestModel, file);
        ApiResponse<RequestHistoryModel> response = new ApiResponse<>(true, "Request submitted successfully", submittedRequest);
        return ResponseEntity.ok(response);
    }




    @GetMapping("/request-history/{applicationId}")
    public ResponseEntity<ApiResponse<?>> getRequestHistory(@PathVariable UUID applicationId){
        List<RequestHistoryModel> requestHistory = candidateThreadService.getCandidateRequestHistory(List.of(applicationId));
        ApiResponse<List<RequestHistoryModel>> response = new ApiResponse<>(true, "Request history fetched successfully", requestHistory);
        return ResponseEntity.ok(response);
    }
}
