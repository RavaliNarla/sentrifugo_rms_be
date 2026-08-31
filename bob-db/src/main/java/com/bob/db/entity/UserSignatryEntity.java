package com.bob.db.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.util.UUID;

@Entity
@Table(name = "user_signatry", schema = "hr")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE hr.user_signatry SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class UserSignatryEntity extends BaseEntity<UUID> {

    @Column(name = "signatry_user_name", nullable = false)
    private String signatryUserName;
}