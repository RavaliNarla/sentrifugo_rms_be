package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AuditTrailDTO extends BaseDTO implements Serializable {
    @JsonProperty("auditId")
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private UUID changedBy;
    private LocalDateTime changeDate;
    private String changeType;
}
