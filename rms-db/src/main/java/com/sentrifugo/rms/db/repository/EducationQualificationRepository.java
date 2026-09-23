package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.EducationQualificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EducationQualificationRepository extends JpaRepository<EducationQualificationEntity, UUID> {
    List<EducationQualificationEntity> findAllByOrderByNameAsc();
    List<EducationQualificationEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    // Paginated variants for the admin list screen (the unpaginated methods above stay as-is
    // since they're also used to populate dropdowns elsewhere, which need the full list).
    Page<EducationQualificationEntity> findAllByOrderByNameAsc(Pageable pageable);
    Page<EducationQualificationEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
