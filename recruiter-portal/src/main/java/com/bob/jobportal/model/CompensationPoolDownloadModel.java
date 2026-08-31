package com.bob.jobportal.model;

import com.bob.db.util.excel.ExcelHeader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompensationPoolDownloadModel {
    @ExcelHeader("Full Name")
    private String fullName;

    @ExcelHeader("Application Number")
    private String applicationNo;

    @ExcelHeader("Current CTC")
    private String currentCtc;

    @ExcelHeader("Expected CTC")
    private String expectedCtc;

    @ExcelHeader("Expected Hike")
    private String hike;

    @ExcelHeader("Agreed CTC")
    private String agreedCtc;

    @ExcelHeader("Comments")
    private String comments;

    @ExcelHeader("Status")
    private String negotiation;


}
