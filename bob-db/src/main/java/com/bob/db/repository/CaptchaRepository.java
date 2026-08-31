package com.bob.db.repository;

import com.bob.db.entity.CaptchaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CaptchaRepository extends JpaRepository<CaptchaEntity, UUID> {

    @Modifying
    @Query("""
    UPDATE CaptchaEntity c
    SET c.isActive = false
    WHERE c.expiryDate < :expiryDate
    """)
    int deleteByExpiryDateBefore(@Param("expiryDate") LocalDateTime expiryDate);
}
