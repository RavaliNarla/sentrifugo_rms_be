package com.bob.jobportal.model;

import com.bob.commonutil.model.candidateportal.RequestHistoryModel;
import com.bob.db.entity.CandidateProfileEntity;
import com.bob.db.enums.ConversationThreadsStatus;
import com.bob.db.enums.SenderTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class MessageResponseModel {
    private UUID conversationMessageId;
    private Map<UUID,String> userName;
    private LocalDateTime createdDate;
    private String comments;
    private SenderTypeEnum senderType;
    private String attachmentPath;
}
