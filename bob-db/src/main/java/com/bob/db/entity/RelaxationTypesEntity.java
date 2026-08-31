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

@Entity
@Table(name = "relaxation_types",schema = "recruitment")
@Data
@SQLDelete(sql = "UPDATE recruitment.relaxation_types SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class RelaxationTypesEntity  extends BaseEntity<UUID>{

    @Column(name = "relaxation_type_name")
    private String relaxationTypeName;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "others", columnDefinition = "jsonb")
    private JsonNode others;

    @Column(name = "input")
    private String input;

    @Column(name = "operator")
    private String operator;

}
