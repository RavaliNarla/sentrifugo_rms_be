package com.bob.db.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.UUID;

@Entity
@Table(name = "job_relaxation_policy",schema = "recruitment")
@Data
@SQLDelete(sql = "UPDATE recruitment.job_relaxation_policy SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobRelaxationPolicyEntity  extends BaseEntity<UUID>{

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "relaxation", columnDefinition = "jsonb", nullable = false)
    private JsonNode relaxation;

    @Column(name="relaxation_policy_number")
    private String relaxationPolicyNumber;
}
