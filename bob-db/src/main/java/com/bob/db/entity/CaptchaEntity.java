package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "captcha", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE hr.captcha SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CaptchaEntity extends BaseEntity<UUID>{
    @Column(name = "captcha_string", nullable = false, length = 255)
    private String captchaString;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

}
