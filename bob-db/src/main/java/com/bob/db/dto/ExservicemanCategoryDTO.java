package com.bob.db.dto;

import java.sql.Timestamp;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
public class ExservicemanCategoryDTO extends BaseDTO {

    @JsonProperty("exServicemanCategoryId")
    private UUID id;
    private String exsCategoryCode;
    private String exsCategoryName;
    private Integer displayOrder;

}
