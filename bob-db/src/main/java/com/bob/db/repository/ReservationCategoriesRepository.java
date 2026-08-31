package com.bob.db.repository;

import com.bob.db.entity.ReservationCategoriesEntity;
import com.bob.db.entity.SpecialCategoriesEntity;
import com.bob.db.enums.ReservationType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationCategoriesRepository extends JpaRepository<ReservationCategoriesEntity, UUID> {
    List<ReservationCategoriesEntity> findAllByIsActiveTrue();

    List<ReservationCategoriesEntity> findAllByIdIn(List<UUID> ids);

    List<ReservationCategoriesEntity> findByReservationType(ReservationType reservationType, Sort by);

    List<ReservationCategoriesEntity> findByCategoryCodeIn(List<String> categoryCodes);

    Optional<ReservationCategoriesEntity> findByCategoryCodeIgnoreCase(String categoryCode);
}
