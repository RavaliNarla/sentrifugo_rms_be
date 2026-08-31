package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;
import java.io.Serializable;

@Data
public class BulkResumeUploadDTO extends BaseDTO implements Serializable {

    @JsonProperty("uploadId")
    private UUID id;

    private String originalFilename;

    private String status;

    private String fileUrl;

    private String failedReason;
}
