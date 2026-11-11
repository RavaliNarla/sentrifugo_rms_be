package com.sentrifugo.rms.authportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RmsAdminActiveRequest {

    @NotNull(message = "active is required")
    private Boolean active;
}
