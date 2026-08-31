package com.bob.authapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class GetUserResponse {

    private UUID userId;
    private String name;
    private String email;
    private String role;
    @JsonProperty("manager_id")
    private UUID managerId;
    @JsonProperty("manager_depth")
    private int managerDepth;
    private Map<String, Boolean> privileges;

}
