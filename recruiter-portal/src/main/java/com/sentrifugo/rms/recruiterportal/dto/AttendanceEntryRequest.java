package com.sentrifugo.rms.recruiterportal.dto;

import com.sentrifugo.rms.db.enums.AttendanceEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class AttendanceEntryRequest {

    @NotBlank(message = "empCode is required")
    @Size(max = 32, message = "empCode must be 32 characters or fewer")
    private String empCode;

    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    @NotNull(message = "type is required")
    private AttendanceEntryType type;
}
