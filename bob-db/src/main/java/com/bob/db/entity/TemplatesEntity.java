package com.bob.db.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "templates",schema = "common")
@Data
@SQLDelete(sql = "UPDATE common.templates SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class TemplatesEntity  extends BaseEntity<UUID>{

    @Column(name = "template_type")
    private String templateType;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "template_desc", columnDefinition = "text")
    private String templateDesc;

    @Column(name = "file_path", columnDefinition = "text")
    private String filePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "others", columnDefinition = "jsonb")
    private JsonNode others;

}
