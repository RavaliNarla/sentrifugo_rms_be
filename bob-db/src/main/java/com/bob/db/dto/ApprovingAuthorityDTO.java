package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class ApprovingAuthorityDTO extends BaseDTO implements Serializable {

    @JsonProperty("approvingAuthorityId")
    private UUID id;

    private String authorityName;

}
