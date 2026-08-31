//package com.bob.db.dto;
//
//import com.bob.db.entity.CityEntity;
//import com.bob.db.entity.DistrictEntity;
//import com.bob.db.enums.RegexPattern;
//import com.bob.db.util.excel.ExcelDropdown;
//import com.bob.db.util.excel.ExcelHeader;
//import com.bob.db.util.excel.RegexValidate;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//
//import java.time.LocalDateTime;
//import java.io.Serializable;
//import java.util.UUID;
//
//@Data
//public class LocationDTO extends BaseDTO implements Serializable {
//
//    @JsonProperty("locationId")
//    private UUID id;
//
//    @ExcelHeader("Location Name")
//    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE ,message = "Field should only contain alphabets,numbers and spaces")
//    private String locationName;
//
//    @ExcelHeader("City Name")
//    @ExcelDropdown(masterClass = CityEntity.class, displayField = "cityName")
//    private UUID cityId;
//
//}
