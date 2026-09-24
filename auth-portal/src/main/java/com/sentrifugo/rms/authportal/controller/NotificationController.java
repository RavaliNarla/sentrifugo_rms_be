package com.sentrifugo.rms.authportal.controller;

import com.sentrifugo.rms.authportal.dto.NotificationDTO;
import com.sentrifugo.rms.authportal.dto.NotificationFeedResponse;
import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.common.service.NotificationService;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.NotificationEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Notifications")
@RestController
@RequestMapping("${auth.api.base.path}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "List in-app notifications from the last 3 days for the current user")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationFeedResponse>> list() {
        UUID userId = securityUtils.getCurrentUserId();
        List<NotificationDTO> items = notificationService.listForUser(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        long unread = notificationService.countUnread(userId);
        return ResponseEntity.ok(ApiResponse.ok(
                NotificationFeedResponse.builder().unreadCount(unread).items(items).build(),
                "Notifications fetched successfully"));
    }

    @Operation(summary = "Mark all visible (last 3 days) notifications as read — clears the unread badge permanently for those items")
    @PostMapping("/mark-read")
    public ResponseEntity<ApiResponse<Void>> markRead() {
        notificationService.markAllVisibleAsRead(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Notifications marked as read"));
    }

    private NotificationDTO toDto(NotificationEntity entity) {
        return NotificationDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .message(entity.getMessage())
                .createdDate(entity.getCreatedDate())
                .unread(entity.getReadAt() == null)
                .build();
    }
}
