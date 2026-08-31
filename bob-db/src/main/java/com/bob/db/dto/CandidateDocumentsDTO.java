package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
public class CandidateDocumentsDTO extends BaseDTO implements Serializable {

//    private UUID candidateId;

    private String documentType;

    private UUID applicationId;

    private String fileName;

    private String fileUrl;

    private LocalDateTime uploadedDate;

    private String comments;

    private String offerStatus;
    private UUID medicalCentreId;
}
