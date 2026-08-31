package com.bob.db.dto;

import com.bob.db.enums.ConversationThreadsStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ConversationThreadsDTO extends BaseDTO implements Serializable {

    @JsonProperty("conversationThreadId")
    private UUID id;

    private UUID applicationId;

    private UUID requestTypeId;

    private String initiatedBy;

    private ConversationThreadsStatus status;

    private LocalDateTime dateExtension;

    private UUID zonalId;

}

