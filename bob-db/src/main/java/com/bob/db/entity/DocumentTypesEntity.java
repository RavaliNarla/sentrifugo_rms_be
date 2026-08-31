package com.bob.db.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.util.UUID;


@Entity
@Table(name="document_types",schema = "candidate")
@Data
@SQLDelete(sql = "UPDATE candidate.document_types SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class DocumentTypesEntity  extends BaseEntity<UUID>{

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "document_desc")
    private String documentDesc;

    @Column(name = "doc_code")
    private String docCode;
  
    @Column(name = "doc_type")
    private String docType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private JsonNode keywords;

    @Column(name = "is_editable")
    private boolean isEditable;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "score")
    private Integer score;

}
