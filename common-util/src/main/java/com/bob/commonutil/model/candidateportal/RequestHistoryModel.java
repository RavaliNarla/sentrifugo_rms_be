package com.bob.commonutil.model.candidateportal;

import com.bob.db.dto.ConversationMessagesDTO;
import com.bob.db.dto.ConversationThreadsDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RequestHistoryModel {
    private ConversationThreadsDTO conversationThreads;
    private List<ConversationMessagesDTO> conversationMessages;
}
