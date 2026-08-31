package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.io.Serializable;
import java.util.UUID;

@Data
public class SkillDTO extends BaseDTO implements Serializable {

    @JsonProperty("skillId")
    private UUID id;

    private String skillName;

    private String skillDesc;

    private LocalDate startDate;

    private LocalDate endDate;

}
