package com.sentrifugo.rms.authportal.controller;

import com.sentrifugo.rms.authportal.model.CurrentUserResponse;
import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Get Details")
@RestController
@RequestMapping("${auth.api.base.path}/getdetails")
@RequiredArgsConstructor
public class GetDetailsController {

    private final SecurityUtils securityUtils;

    @Operation(summary = "Get the currently signed-in user and their screen privileges")
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser() {
        UserEntity user = securityUtils.getCurrentUser();
        CurrentUserResponse response = CurrentUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .employeeId(user.getEmployeeId())
                .privileges(securityUtils.getPrivileges(user.getId()))
                .build();
        return ResponseEntity.ok(ApiResponse.ok(response, "Current user fetched successfully"));
    }
}
