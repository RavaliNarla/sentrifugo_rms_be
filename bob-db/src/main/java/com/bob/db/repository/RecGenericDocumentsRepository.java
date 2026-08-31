package com.bob.db.repository;

import com.bob.db.entity.RecGenericDocumentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecGenericDocumentsRepository extends JpaRepository<RecGenericDocumentsEntity, UUID> {
    Optional<RecGenericDocumentsEntity> findTopByTypeOrderByVersionNoDesc(String type);

    @Query("""
    SELECT r
    FROM RecGenericDocumentsEntity r
    WHERE r.versionNo = (
        SELECT MAX(r2.versionNo)
        FROM RecGenericDocumentsEntity r2
        WHERE r2.type = r.type
    )
    ORDER BY r.type""")
    List<RecGenericDocumentsEntity> findLatestOnePerType();

    Optional<RecGenericDocumentsEntity> findByType(String type);
}
