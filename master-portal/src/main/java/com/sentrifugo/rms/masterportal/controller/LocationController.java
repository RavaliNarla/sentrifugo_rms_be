package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.masterportal.dto.LocationDTO;
import com.sentrifugo.rms.masterportal.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Locations")
@RestController
@RequestMapping("${master.api.base.path}/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @Operation(summary = "Full unpaginated list - used to populate the Add Position Location dropdown")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<LocationDTO>>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.getAll(search), "Locations fetched successfully"));
    }

    @Operation(summary = "Paginated + searchable list - drives the Locations admin screen")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<LocationDTO>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.search(search, page, size), "Locations fetched successfully"));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<LocationDTO>> add(@Valid @RequestBody LocationDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.add(dto), "Location added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<LocationDTO>> update(@PathVariable UUID id, @Valid @RequestBody LocationDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.update(id, dto), "Location updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        locationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Location deleted successfully"));
    }
}
