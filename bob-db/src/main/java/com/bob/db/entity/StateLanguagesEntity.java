package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;
@Entity
@Table(name = "state_languages",schema = "common")
@Data
@EntityListeners(AuditingEntityListener.class)
//@SQLDelete(sql = "UPDATE common.state_languages SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateLanguagesEntity extends BaseEntity<UUID>{

    @Column(name = "state_id", nullable = false)
    private UUID stateId;

    @Column(name = "language_id", nullable = false)
    private UUID languageId;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;


}
