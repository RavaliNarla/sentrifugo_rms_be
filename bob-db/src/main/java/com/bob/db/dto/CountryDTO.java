package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class CountryDTO extends BaseDTO implements Serializable {

    @JsonProperty("countryId")
    private UUID id;

    private String countryName;

}
