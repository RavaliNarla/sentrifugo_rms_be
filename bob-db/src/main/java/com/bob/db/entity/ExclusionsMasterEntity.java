package com.bob.db.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "exclusions_master", schema = "recruitment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE recruitment.exclusions_master SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ExclusionsMasterEntity extends BaseEntity<UUID> {

    @Column(name = "exclusion_type", nullable = false, length = 50)
    private String exclusionType;

    @Column(name = "exclusion_value", nullable = false, length = 255)
    private String exclusionValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private JsonNode keywords;

}

