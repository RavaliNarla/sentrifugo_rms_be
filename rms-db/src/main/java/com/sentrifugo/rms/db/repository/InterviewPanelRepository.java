package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.InterviewPanelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewPanelRepository extends JpaRepository<InterviewPanelEntity, UUID> {
    // Unpaginated - used by the Interview Schedules filter and the Schedule Interview panel dropdown.
    List<InterviewPanelEntity> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);

    // Paginated + searchable - drives the Manage Panels admin screen.
    @Query("SELECT p FROM InterviewPanelEntity p WHERE :search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<InterviewPanelEntity> search(@Param("search") String search, Pageable pageable);
}
