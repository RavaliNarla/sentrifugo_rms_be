package com.bob.commonutil.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ScreeningCommentsResponseModel {
    private UUID applicationId;
    private List<ScreeningCommentResponseModel> comments;
}

