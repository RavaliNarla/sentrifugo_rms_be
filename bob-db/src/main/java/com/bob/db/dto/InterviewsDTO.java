package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.io.Serializable;

@Data
public class InterviewsDTO extends BaseDTO implements Serializable {

    @JsonProperty("interviewId")
    private UUID id;

    private UUID applicationId;

    private LocalDate date;

    private LocalTime time;

    private String status;

    private String type;

    private Boolean isPanelInterview;

    private String phone;

    private String location;

}
