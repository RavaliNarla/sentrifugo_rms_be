package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.masterportal.dto.UserDTO;
import com.sentrifugo.rms.masterportal.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Users")
@RestController
@RequestMapping("${master.api.base.path}/user")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Paginated + searchable list - drives the Users admin screen")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAll(search, page, size), "Users fetched successfully"));
    }

    @Operation(summary = "Add a user (generates Employee ID; password required for login)")
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<UserDTO>> add(@Valid @RequestBody UserDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(userService.add(dto), "User added successfully"));
    }

    @Operation(summary = "Update a user's name/role (optional password change)")
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> update(@PathVariable UUID id, @Valid @RequestBody UserDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, dto), "User updated successfully"));
    }

    @Operation(summary = "Delete (deactivate) a user")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully"));
    }
}
