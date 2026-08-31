package com.bob.db.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;

import java.util.UUID;

@Data
public class UserDTO extends BaseDTO implements Serializable {
    @JsonProperty("userId")
    private UUID id;

    private String name;

    private String role;

    private String email;

    private UUID interviewCenterId;

}