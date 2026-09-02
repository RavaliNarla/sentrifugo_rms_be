package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
    List<LocationEntity> findAllByOrderByNameAsc();
}
