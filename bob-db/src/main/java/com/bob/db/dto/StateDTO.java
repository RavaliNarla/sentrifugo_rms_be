package com.bob.db.dto;

import com.bob.db.entity.CountryEntity;
import com.bob.db.entity.DistrictEntity;
import com.bob.db.util.excel.ExcelDropdown;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class StateDTO extends BaseDTO implements Serializable {

    @JsonProperty("stateId")
    private UUID id;

    private String stateName;

    @ExcelDropdown(masterClass = CountryEntity.class, displayField = "countryName")
    private UUID countryId;

    private String localLanguage;


}

