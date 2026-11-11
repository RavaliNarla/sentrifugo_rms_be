package com.sentrifugo.rms.authportal.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RmsAdminDTO {
    private UUID id;
    private String name;
    private String email;
    private String employeeId;
    private Boolean isActive;
}
