package com.bob.authapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AllUsersResponse {

    private UUID userId;
    private String name;
    private String role;
    private String email;
    private UUID interviewCenterId;

}
