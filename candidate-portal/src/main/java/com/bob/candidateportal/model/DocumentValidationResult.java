package com.bob.candidateportal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentValidationResult {
    private String status; // "validated", "pending", or "rejected"
    private List<String> pendingChecks; // Array of missing items: e.g. ["document number"], ["date of birth"], ["company name", "from date"]
    
    // Constructor for backward compatibility
    public DocumentValidationResult(String status) {
        this.status = status;
        this.pendingChecks = null;
    }
}
