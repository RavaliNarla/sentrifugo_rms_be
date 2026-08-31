package com.bob.db.dto;

import com.bob.db.enums.CcAvenueOrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CcAvenueOrdersDTO extends BaseDTO{
    
	@JsonProperty("id")
    private UUID id;
    
	@JsonProperty("order_id")
    private UUID orderId;
    
	@JsonProperty("tracking_id")
    private String trackingId;
    
	@JsonProperty("bank_ref_no")
    private String bankRefNo;
    
	@JsonProperty("amount")
    private BigDecimal amount;
    
	@JsonProperty("currency")
    private String currency;
    
	@JsonProperty("order_status")
    private CcAvenueOrderStatus status;
    
	@JsonProperty("payment_mode")
    private String paymentMode;
    
	@JsonProperty("card_name")
    private String cardName;
    
	@JsonProperty("trans_date")
    private String transDate;
    
	@JsonProperty("status_code")
    private String statusCode;
    
	@JsonProperty("status_message")
    private String statusMessage;
    
	@JsonProperty("failure_message")
    private String failureMessage;
    
	@JsonProperty("response_code")
    private String responseCode;
    
	@JsonProperty("billing_name")
    private String billingName;
    
	@JsonProperty("billing_address")
    private String billingAddress;
    
	@JsonProperty("billing_city")
    private String billingCity;
    
	@JsonProperty("billing_state")
    private String billingState;
    
	@JsonProperty("billing_zip")
    private String billingZip;
    
	@JsonProperty("billing_country")
    private String billingCountry;
    
	@JsonProperty("billing_tel")
    private String billingTel;
    
	@JsonProperty("billing_email")
    private String billingEmail;
    
	@JsonProperty("billing_notes")
    private String billingNotes;
    
	@JsonProperty("delivery_name")
    private String deliveryName;
    
	@JsonProperty("delivery_address")
    private String deliveryAddress;
    
	@JsonProperty("delivery_city")
    private String deliveryCity;
    
	@JsonProperty("delivery_state")
    private String deliveryState;
    
	@JsonProperty("delivery_zip")
    private String deliveryZip;
    
	@JsonProperty("delivery_country")
    private String deliveryCountry;
    
	@JsonProperty("delivery_tel")
    private String deliveryTel;
    
	@JsonProperty("merchant_param1")
    private String merchantParam1;
    
	@JsonProperty("merchant_param2")
    private String merchantParam2;
    
	@JsonProperty("merchant_param3")
    private String merchantParam3;
    
	@JsonProperty("merchant_param4")
    private String merchantParam4;
    
	@JsonProperty("merchant_param5")
    private String merchantParam5;
    
	@JsonProperty("offer_type")
    private String offerType;
    
	@JsonProperty("offer_code")
    private String offerCode;
    
	@JsonProperty("discount_value")
    private BigDecimal discountValue;
    
	@JsonProperty("mer_amount")
    private BigDecimal merAmount;
    
	@JsonProperty("eci_value")
    private String eciValue;
    
	@JsonProperty("retry")
    private String retry;
    
	@JsonProperty("bin_country")
    private String binCountry;
    
	@JsonProperty("auth_ref_num")
    private String authRefNum;

    @JsonProperty("application_id")
    private String applicationId;
}