package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.LocationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
    List<LocationEntity> findAllByOrderByNameAsc();

    @Query("""
        SELECT l FROM LocationEntity l
        WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(l.address) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY l.name ASC
        """)
    List<LocationEntity> search(@Param("search") String search);

    // Paginated variants for the admin list screen (the unpaginated methods above stay as-is
    // since they're also used to populate the Add Position Location dropdown).
    Page<LocationEntity> findAllByOrderByNameAsc(Pageable pageable);

    @Query("""
        SELECT l FROM LocationEntity l
        WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(l.address) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY l.name ASC
        """)
    Page<LocationEntity> search(@Param("search") String search, Pageable pageable);
}
