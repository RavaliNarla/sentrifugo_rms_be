package com.bob.jobportal.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class SendOfferRequestModel {

    private UUID offerTemplateId;

    private LocalDate joiningDate;

    private LocalDate acceptBeforeDate;

    private UUID designationId;

//    private UUID userSignatryId;

    private List<UUID> offerIds;

    private String signatoryName;

    private String signatoryDesignation;
}
