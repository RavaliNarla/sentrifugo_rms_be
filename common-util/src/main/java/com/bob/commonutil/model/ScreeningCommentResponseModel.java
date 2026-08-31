package com.bob.commonutil.model;

import com.bob.db.enums.ScreeningCommentUserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ScreeningCommentResponseModel {
    private UUID id;
    private UUID userId;
    private ScreeningCommentUserRole userRole;
    private String commentText;
    private LocalDateTime createdDate;
}

