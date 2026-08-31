package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RoleDTO extends BaseDTO implements Serializable {
    private String roleCode;

    private String roleName;

    private String roleDescription;

    private String roleCategory;
}
