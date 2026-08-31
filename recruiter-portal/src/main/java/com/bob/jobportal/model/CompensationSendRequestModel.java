package com.bob.jobportal.model;

import com.bob.db.dto.InterviewScheduleDTO;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CompensationSendRequestModel {
    private List<InterviewScheduleDTO> interviewSchedules;
    private LocalDate submitBeforeDate;
}
