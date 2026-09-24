package com.sentrifugo.rms.authportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationFeedResponse {
    private long unreadCount;
    private List<NotificationDTO> items;
}
