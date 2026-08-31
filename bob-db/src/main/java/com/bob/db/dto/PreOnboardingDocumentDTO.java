package com.bob.db.dto;

import com.bob.db.enums.PreOnboardingDocumentType;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class PreOnboardingDocumentDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private String referenceTable;

    private UUID referenceId;

    private PreOnboardingDocumentType documentType;

    private String documentName;

    private String documentUrl;

}