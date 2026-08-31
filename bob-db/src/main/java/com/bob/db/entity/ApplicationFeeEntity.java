package com.bob.db.entity;

import com.bob.db.enums.ApplicationFeeCategoryCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(schema = "finance", name = "application_fee")
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@SQLDelete(sql = "UPDATE common.approving_authority SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ApplicationFeeEntity extends BaseEntity<UUID>{

    @Column(name = "fee_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal feeAmount;

    @Column(name = "category_code", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationFeeCategoryCode categoryCode;
}
