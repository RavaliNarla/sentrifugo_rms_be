package com.bob.candidateportal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkExpFieldValidationResult {
    private boolean fieldsFound;
    private List<String> missingFields;
}
