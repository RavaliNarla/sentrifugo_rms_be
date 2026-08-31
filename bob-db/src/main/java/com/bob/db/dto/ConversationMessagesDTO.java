package com.bob.db.dto;

import com.bob.db.enums.SenderTypeEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ConversationMessagesDTO extends BaseDTO implements Serializable {

    @JsonProperty("conversationMessageId")
    private UUID id;

    private UUID threadId;

    private SenderTypeEnum senderType;

    private UUID senderId;

    private String message;

    private String attachmentPath;

    private Boolean isRead = false;

}

