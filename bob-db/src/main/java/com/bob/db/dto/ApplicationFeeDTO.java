package com.bob.db.dto;

import com.bob.db.enums.ApplicationFeeCategoryCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationFeeDTO extends BaseDTO {

    @JsonProperty("applicationFeeId")
    private UUID id;

    private BigDecimal feeAmount;

    private ApplicationFeeCategoryCode categoryCode;

}
