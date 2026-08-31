package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "request_types", schema = "common")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE common.request_types SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class RequestTypesEntity extends BaseEntity<UUID> {

    @Column(name = "request_name", length = 255, nullable = false)
    private String requestName;

}
