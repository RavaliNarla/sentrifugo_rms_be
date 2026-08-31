package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_grade",schema = "recruitment")
@Data
@SQLDelete(sql = "UPDATE recruitment.job_grade SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobGradeEntity  extends BaseEntity<UUID>{

    @Column(name = "job_grade_code")
    private String jobGradeCode;

    @Column(name = "job_grade_desc", columnDefinition = "text")
    private String jobGradeDesc;

    @Column(name = "job_scale")
    private String jobScale;

    @Column(name = "min_salary", precision = 12, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "max_salary", precision = 12, scale = 2)
    private BigDecimal maxSalary;

    @Column(name = "effective_state_date")
    private LocalDate effectiveStateDate;

    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;


}
