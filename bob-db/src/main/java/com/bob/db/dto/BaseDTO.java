package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public abstract class BaseDTO implements Serializable {

    @JsonIgnore
    private UUID createdBy;

    @JsonIgnore
    private UUID modifiedBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime modifiedDate;

    @JsonIgnore
    private Boolean isActive = true ;

}
