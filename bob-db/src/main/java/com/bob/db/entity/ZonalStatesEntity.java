package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;


@Entity
@Table(name = "zonal_states", schema = "common")
@SQLDelete(sql = "UPDATE common.zonal_states SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@Data
public class ZonalStatesEntity extends BaseEntity<UUID> {

    @Column(name = "state_name", length = 100)
    private String stateName;

}
