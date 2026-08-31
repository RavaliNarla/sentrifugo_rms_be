package com.bob.jobportal.model;

import com.bob.db.enums.ConversationThreadsStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MessageApprovalRequestModel {
    private List<UUID> conversationThreadId;
    private ConversationThreadsStatus status;
    private String comments;
}
