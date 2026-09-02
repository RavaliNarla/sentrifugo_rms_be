package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.InterviewPanelDTO;
import com.sentrifugo.rms.recruiterportal.service.InterviewPanelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Committee Management - Panels")
@RestController
@RequestMapping("${recruiter.api.base.path}/interview-panels")
@RequiredArgsConstructor
public class InterviewPanelController {

    private final InterviewPanelService interviewPanelService;

    @Operation(summary = "Create an interview panel with members")
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<InterviewPanelDTO>> create(@Valid @RequestBody InterviewPanelDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(interviewPanelService.create(dto), "Panel created successfully"));
    }

    @Operation(summary = "Update a panel's name/members")
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<InterviewPanelDTO>> update(@PathVariable UUID id, @Valid @RequestBody InterviewPanelDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(interviewPanelService.update(id, dto), "Panel updated successfully"));
    }

    @Operation(summary = "Delete a panel (blocked if assigned to a position)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        interviewPanelService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Panel deleted successfully"));
    }

    @Operation(summary = "List all panels")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<InterviewPanelDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(interviewPanelService.getAll(), "Panels fetched successfully"));
    }
}
