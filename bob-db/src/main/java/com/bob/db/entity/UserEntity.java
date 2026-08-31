package com.bob.db.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.util.UUID;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", schema = "hr")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@SQLDelete(sql = "UPDATE hr.users SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class UserEntity  extends BaseEntity<UUID>{

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "role")
    private String role;


    @Column(name = "email")
    private String email;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "step_level")
    private Integer stepLevel;

    @Column(name = "user_password")
    private String userPassword;

    @Column(name = "oath_user_id")
    private String oathUserId;

    @Column(name = "interview_center_id")
    private UUID interviewCenterId;
}