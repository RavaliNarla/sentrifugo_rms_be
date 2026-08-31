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
@Table(name = "medical_centres", schema = "common")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE common.medical_centres SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class MedicalCentresEntity extends BaseEntity<UUID> {

    @Column(name = "medical_centre")
    private String medicalCentre;

}
