package com.bob.jobportal.model;

import com.bob.db.enums.ConversationThreadsStatus;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MessageThreadRequestModel {

    @NotEmpty(message = "Positions cannot be empty")
    private List<UUID> positionsIds;

    private List<UUID> requestTypeIds;

    private List<ConversationThreadsStatus> statusList;

    private String searchText;
}
