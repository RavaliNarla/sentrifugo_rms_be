package com.bob.db.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecGenericDocumentsDTO extends BaseDTO {

    private UUID id;
    private String type;
    private String fileName;
    private String fileUrl;
    private Integer versionNo;
    private String displayName;



}

