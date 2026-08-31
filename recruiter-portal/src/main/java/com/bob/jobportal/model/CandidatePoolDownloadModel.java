package com.bob.jobportal.model;

import com.bob.db.util.excel.ExcelHeader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidatePoolDownloadModel {
    @ExcelHeader("Position")
    private String position;

    @ExcelHeader("Full Name")
    private String fullName;

    @ExcelHeader("Application Number")
    private String applicationNo;

    @ExcelHeader("Rank")
    private Integer rank;

    @ExcelHeader("Score")
    private BigDecimal finalScore;

    @ExcelHeader("Work Experience")
    private Float workExperience;

    @ExcelHeader("Application Status")
    private String status;

    @ExcelHeader("Location")
    private String location;

    @ExcelHeader("Category")
    private String category;
}
