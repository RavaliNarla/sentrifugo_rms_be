package com.bob.db.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.io.Serializable;

@Data
public class CandidateDocumentStoreDTO extends BaseDTO implements Serializable {
    private UUID id;

    private UUID candidateId;

    private UUID documentId;

    private String fileName;

    private String fileUrl;

    private LocalDateTime uploadedDate;

    private boolean isDigilocker;

    private UUID documentIdentifier;

    private String displayName;

    private String documentNumber;

    private Boolean isValidationPending;

    private List<String> pendingChecks;

}
