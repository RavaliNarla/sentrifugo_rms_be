package com.bob.db.entity;

import com.bob.db.enums.CcAvenueOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cc_avenue_orders", schema = "finance")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE finance.cc_avenue_orders SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CcAvenueOrdersEntity extends BaseEntity<UUID> {

    
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "tracking_id", length = 100)
    private String trackingId;

    @Column(name = "bank_ref_no", length = 100)
    private String bankRefNo;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING) // Persist enum as String
    @Column(name = "status", length = 50)
    private CcAvenueOrderStatus status;

    @Column(name = "payment_mode", length = 100)
    private String paymentMode;

    @Column(name = "card_name", length = 100)
    private String cardName;

    @Column(name = "trans_date", length = 50)
    private String transDate;

    @Column(name = "status_code", length = 50)
    private String statusCode;

    @Column(name = "status_message", columnDefinition = "TEXT")
    private String statusMessage;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "response_code", length = 50)
    private String responseCode;

    @Column(name = "billing_name", length = 255)
    private String billingName;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 100)
    private String billingState;

    @Column(name = "billing_zip", length = 20)
    private String billingZip;

    @Column(name = "billing_country", length = 100)
    private String billingCountry;

    @Column(name = "billing_tel", length = 20)
    private String billingTel;

    @Column(name = "billing_email", length = 255)
    private String billingEmail;

    @Column(name = "billing_notes", columnDefinition = "TEXT")
    private String billingNotes;

    @Column(name = "delivery_name", length = 255)
    private String deliveryName;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "delivery_state", length = 100)
    private String deliveryState;

    @Column(name = "delivery_zip", length = 20)
    private String deliveryZip;

    @Column(name = "delivery_country", length = 100)
    private String deliveryCountry;

    @Column(name = "delivery_tel", length = 20)
    private String deliveryTel;

    @Column(name = "merchant_param1", columnDefinition = "TEXT")
    private String merchantParam1;

    @Column(name = "merchant_param2", columnDefinition = "TEXT")
    private String merchantParam2;

    @Column(name = "merchant_param3", columnDefinition = "TEXT")
    private String merchantParam3;

    @Column(name = "merchant_param4", columnDefinition = "TEXT")
    private String merchantParam4;

    @Column(name = "merchant_param5", columnDefinition = "TEXT")
    private String merchantParam5;

    @Column(name = "offer_type", length = 50)
    private String offerType;

    @Column(name = "offer_code", length = 50)
    private String offerCode;

    @Column(name = "discount_value", precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "mer_amount", precision = 12, scale = 2)
    private BigDecimal merAmount;

    @Column(name = "eci_value", length = 20)
    private String eciValue;

    @Column(name = "retry", length = 1)
    private String retry;

    @Column(name = "bin_country", length = 100)
    private String binCountry;

    @Column(name = "auth_ref_num", length = 100)
    private String authRefNum;

    @Column(name="application_id")
    private UUID applicationId;
}
