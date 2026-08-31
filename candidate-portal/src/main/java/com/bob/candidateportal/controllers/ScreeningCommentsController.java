package com.bob.candidateportal.controllers;

import com.bob.commonutil.model.CreateScreeningCommentRequestModel;
import com.bob.commonutil.model.ScreeningCommentsResponseModel;
import com.bob.commonutil.service.ScreeningCommentsService;
import com.bob.db.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/screening-comments")
@PreAuthorize("hasAuthority('CANDIDATE')")
public class ScreeningCommentsController {

    @Autowired
    private ScreeningCommentsService screeningCommentsService;

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<ScreeningCommentsResponseModel>> getComments(@PathVariable UUID applicationId) {
        ScreeningCommentsResponseModel response = screeningCommentsService.getComments(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Screening comments fetched successfully"));
    }

    @PostMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<ScreeningCommentsResponseModel>> addComment(
            @PathVariable UUID applicationId,
            @Valid @RequestBody CreateScreeningCommentRequestModel request
    ) {
        ScreeningCommentsResponseModel response = screeningCommentsService.addComment(applicationId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Screening comment saved successfully"));
    }
}

