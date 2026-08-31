package com.bob.db.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "certification_master",schema = "common")
@SQLDelete(sql = "UPDATE common.certification_master SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@Data
public class CertificationMasterEntity extends  BaseEntity<UUID>{

    @Column(name = "certification_name",nullable = false,unique = true)
    private String certificationName;

    @Column(name = "certification_desc",columnDefinition = "text")
    private String certificationDesc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private JsonNode keywords;
}
