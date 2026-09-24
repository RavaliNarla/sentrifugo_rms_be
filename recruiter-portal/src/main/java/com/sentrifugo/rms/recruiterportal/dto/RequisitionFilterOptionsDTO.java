package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Distinct values (across all requisitions/positions) used to populate the Job Postings filter dropdowns. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionFilterOptionsDTO {
    private List<Integer> years;
    private List<String> jobTitles;
    private List<String> departments;
    private List<String> locations;
}
