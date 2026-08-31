package com.bob.db.dto;


import com.bob.db.entity.DistrictEntity;
import com.bob.db.entity.StateEntity;
import com.bob.db.util.excel.ExcelDropdown;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.UUID;

@Data
public class CityDTO extends BaseDTO implements Serializable {

    @JsonProperty("cityId")
    private UUID id;

    private String cityName;

    @ExcelDropdown(masterClass = StateEntity.class, displayField = "stateName")
    private UUID stateId;

    @ExcelDropdown(masterClass = DistrictEntity.class, displayField = "districtName")
    private UUID districtId;

}
