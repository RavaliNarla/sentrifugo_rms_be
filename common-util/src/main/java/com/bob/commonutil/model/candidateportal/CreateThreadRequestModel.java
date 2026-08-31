package com.bob.commonutil.model.candidateportal;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data

public class CreateThreadRequestModel {
    private UUID requestTypeId;
    private String description;
    private UUID applicationId;
    private LocalDateTime dateExtension;
    private UUID zonalId;
}
