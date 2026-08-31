package com.bob.commonutil.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateScreeningCommentRequestModel {

    @NotBlank(message = "Comment text is required")
    @Size(max = 2000, message = "Comment text cannot exceed 2000 characters")
    private String commentText;
}

