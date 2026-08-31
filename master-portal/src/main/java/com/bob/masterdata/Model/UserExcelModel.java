package com.bob.masterdata.Model;

import com.bob.db.entity.InterviewCentresEntity;
import com.bob.db.enums.UserRole;
import com.bob.db.util.excel.ExcelDropdown;
import com.bob.db.util.excel.ExcelEnumDropdown;
import com.bob.db.util.excel.ExcelHeader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class UserExcelModel {
    @JsonProperty("userId")
    private UUID id;

    @ExcelHeader(value="Name",isMandatory = true)
    private String name;

    @ExcelHeader(value="Role",isMandatory = true)
    @ExcelEnumDropdown(enumClass = UserRole.class,displayField = "value" )
    private String role;

    @ExcelHeader(value="Email",isMandatory = true)
    private String email;

    @ExcelHeader("Interview Center")
    @ExcelDropdown(masterClass = InterviewCentresEntity.class, displayField = "interviewCentre")
    private UUID interviewCenterId;
}
