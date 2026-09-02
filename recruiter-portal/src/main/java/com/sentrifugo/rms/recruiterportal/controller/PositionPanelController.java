package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.recruiterportal.dto.PositionPanelDTO;
import com.sentrifugo.rms.recruiterportal.service.PositionPanelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Committee Management - Assign to Positions")
@RestController
@RequestMapping("${recruiter.api.base.path}/position-panels")
@RequiredArgsConstructor
public class PositionPanelController {

    private final PositionPanelService positionPanelService;

    @Operation(summary = "Assign a panel to a position with a date range")
    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<PositionPanelDTO>> assign(@Valid @RequestBody PositionPanelDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(positionPanelService.assign(dto), "Panel assigned to position successfully"));
    }

    @Operation(summary = "Remove a panel assignment from a position")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable UUID id) {
        positionPanelService.remove(id);
        return ResponseEntity.ok(ApiResponse.ok("Panel assignment removed successfully"));
    }

    @Operation(summary = "List panels assigned to a position")
    @GetMapping("/by-position/{positionId}")
    public ResponseEntity<ApiResponse<List<PositionPanelDTO>>> getByPosition(@PathVariable UUID positionId) {
        return ResponseEntity.ok(ApiResponse.ok(positionPanelService.getByPositionId(positionId), "Position panels fetched successfully"));
    }

    @Operation(summary = "List active (not-yet-ended) panels assigned to a position - for scheduling")
    @GetMapping("/active-by-position/{positionId}")
    public ResponseEntity<ApiResponse<List<PositionPanelDTO>>> getActiveByPosition(@PathVariable UUID positionId) {
        return ResponseEntity.ok(ApiResponse.ok(positionPanelService.getActiveByPositionId(positionId), "Active position panels fetched successfully"));
    }
}
