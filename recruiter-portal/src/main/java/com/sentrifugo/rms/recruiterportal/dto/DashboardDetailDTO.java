package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Rows for a dashboard metric drill-down modal (matches summary filters exactly). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDetailDTO {
    private String metric;
    private String title;
    private List<String> columns;
    /** Each row is column-name → display value. */
    private List<Map<String, String>> rows;
}
