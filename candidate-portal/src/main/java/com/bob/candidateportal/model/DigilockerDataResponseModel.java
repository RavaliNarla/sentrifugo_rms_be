package com.bob.candidateportal.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DigilockerDataResponseModel {
    private String docCode;
    private Integer pageNo;
    private Boolean isSuccess;
    private String errorMessage;
}
