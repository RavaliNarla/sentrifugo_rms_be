package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByUserIdAndCreatedDateGreaterThanEqualOrderByCreatedDateDesc(
            UUID userId, LocalDateTime since);

    long countByUserIdAndReadAtIsNullAndCreatedDateGreaterThanEqual(UUID userId, LocalDateTime since);

    Optional<NotificationEntity> findByUserIdAndTypeAndReferenceDate(UUID userId, String type, LocalDate referenceDate);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE NotificationEntity n
        SET n.readAt = :readAt
        WHERE n.userId = :userId
          AND n.readAt IS NULL
          AND n.createdDate >= :since
        """)
    int markUnreadAsRead(@Param("userId") UUID userId,
                         @Param("since") LocalDateTime since,
                         @Param("readAt") LocalDateTime readAt);
}
