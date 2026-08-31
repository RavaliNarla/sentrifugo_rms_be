package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "pincode", schema = "common")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE common.pincode SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PincodeEntity extends BaseEntity<UUID> {

    @Column(name = "pin", nullable = false, length = 20)
    private String pin;

    @Column(name = "city_id")
    private UUID cityId;

}
