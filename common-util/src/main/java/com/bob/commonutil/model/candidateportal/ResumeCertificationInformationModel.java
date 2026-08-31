package com.bob.commonutil.model.candidateportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeCertificationInformationModel {
    private String certificateName;
    private String issuedBy;
    private String certificationDate;
    private String certificationExpiryDate;
}
