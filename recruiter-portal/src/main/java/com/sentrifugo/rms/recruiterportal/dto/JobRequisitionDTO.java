package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequisitionDTO {
    private UUID id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Expected fulfilment date is required")
    private LocalDate expectedFulfilmentDate;

    private String status;
    private String requisitionCode;
    private String comments;

    /** Optional scope: when set, positions inherit/lock this department. */
    private UUID departmentId;
    private String departmentName;
    private String departmentCode;

    /** Optional scope: when set, positions inherit/lock this location. */
    private UUID locationId;
    private String locationName;
    private String locationCode;

    private List<JobPositionDTO> positions;
}
