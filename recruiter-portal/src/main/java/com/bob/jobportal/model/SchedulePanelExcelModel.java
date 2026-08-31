package com.bob.jobportal.model;

import com.bob.db.entity.InterviewPanelsEntity;
import com.bob.db.util.excel.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class SchedulePanelExcelModel {


    @ExcelHeader(value = "Select Panel",isMandatory = true)
    @ExcelDropdown(
            masterClass = InterviewPanelsEntity.class,
            displayField = "panelName",
            filters = {
                    @ExcelFilter(
                            field = "isActive",
                            param = "isActive",
                            operator = Operator.EQUAL,
                            logical = LogicalOperator.AND
                    ),

                    @ExcelFilter(
                            field = "committee.id",
                            param = "committeeId",
                            operator = Operator.EQUAL,
                            logical = LogicalOperator.AND
                    ),

                    @ExcelFilter(
                            field = "id",
                            param = "panelIds",
                            operator = Operator.IN,
                            logical = LogicalOperator.AND
                    )
            }
    )
    @NotNull(message = "Panel selection is required")
    private UUID panelId;

    @ExcelHeader(value = "Panel Date (DD-MM-YYYY)",isMandatory = true)
    @NotNull(message = "Panel date is required")
    private LocalDate panelDate;

    @ExcelHeader(value = "Interviews Per Day",isMandatory = true)
    @NotNull(message = "Interviews per day is required")
    private Integer interviewPerDay;

    @ExcelHeader(value = "Start Time (HH:mm)",isMandatory = true)
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @ExcelHeader(value = "Start Time (HH:mm)",isMandatory = true)
    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @ExcelHeader(value = "DurationInMinutes",isMandatory = true)
    @NotNull(message = "Duration is required")
    private Integer durationInMinutes;

}
