package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCompensationDTO extends BaseDTO {

    @JsonProperty("applicationCompensationId")
    private UUID id;
    private UUID applicationId;
    private String employmentType;
    private BigDecimal currentCtc;
    private BigDecimal expectedCtc;
    private String fileName;
    private String fileUrl;
}

