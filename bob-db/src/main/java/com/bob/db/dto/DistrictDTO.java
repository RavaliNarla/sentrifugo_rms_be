package com.bob.db.dto;

import com.bob.db.entity.DistrictEntity;
import com.bob.db.entity.StateEntity;
import com.bob.db.util.excel.ExcelDropdown;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class DistrictDTO extends BaseDTO implements Serializable {

    @JsonProperty("districtId")
    private UUID id;


    private String districtName;

    @ExcelDropdown(masterClass = StateEntity.class, displayField = "stateName")
    private UUID stateId;
}