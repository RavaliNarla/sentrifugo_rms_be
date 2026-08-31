package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "state",schema = "common")
@Data
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE common.state SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class StateEntity  extends BaseEntity<UUID>{

    @Column(name="state_name")
    private String stateName;

    @Column(name="country_id")
    private UUID countryId;

    @Column(name = "local_language", length = 100)
    private String localLanguage;

}
