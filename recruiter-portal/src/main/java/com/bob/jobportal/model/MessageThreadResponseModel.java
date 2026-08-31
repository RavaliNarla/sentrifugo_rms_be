package com.bob.jobportal.model;

import com.bob.db.enums.ConversationThreadsStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageThreadResponseModel {
    // primary Id
    private UUID conversationThreadId;
    private String candidateName;
    private String applicationNo;
    private UUID positionId;
    private UUID applicationId;
    private UUID requestTypeId;
    private ConversationThreadsStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime dateExtension;
    private UUID zonalId;
}
