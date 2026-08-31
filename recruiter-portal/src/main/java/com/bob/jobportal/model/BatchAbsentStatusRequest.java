package com.bob.jobportal.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAbsentStatusRequest {

    @NotEmpty(message = "Absent status updates list cannot be empty")
    @Valid
    private List<AbsentStatusUpdate> absentStatusUpdates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbsentStatusUpdate {
        
        @NotNull(message = "Application ID is required")
        private UUID applicationId;
        
        @NotNull(message = "IsAbsent flag is required")
        private Boolean isAbsent;
    }
}
