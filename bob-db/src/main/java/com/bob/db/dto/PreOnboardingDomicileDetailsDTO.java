package com.bob.db.dto;

import com.bob.db.entity.PreOnboardingDocumentEntity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Data;
import org.hibernate.annotations.Where;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class PreOnboardingDomicileDetailsDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private UUID stateOfOriginId;

    private UUID religionId;

    private Boolean isMinority = false;

    private UUID cityId;

    private UUID stateId;

    private UUID districtId;

    private UUID casteCategoryId;

    private String casteCommunity;

    private String nationality;

    private String bloodGroup;

    private String panNo;

    private Boolean panApplied = false;

    private String panReferenceNo;

    private String mobileNo;

    private String emailAddress;

    private String voterIdCardNo;

    private String drivingLicenseNo;

    private String passportNo;

    private List<PreOnboardingDocumentEntity> documents;
}