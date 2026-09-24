package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByEmployeeIdIgnoreCase(String employeeId);

    @Query("""
        SELECT u FROM UserEntity u
        WHERE LOWER(u.email) = LOWER(:username)
           OR LOWER(u.employeeId) = LOWER(:username)
        """)
    Optional<UserEntity> findByEmailOrEmployeeIdIgnoreCase(@Param("username") String username);

    List<UserEntity> findByRoleIn(List<String> roles);

    // Paginated - drives the Users admin screen.
    Page<UserEntity> findAllByOrderByNameAsc(Pageable pageable);

    @Query("""
        SELECT u FROM UserEntity u
        WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.role) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(COALESCE(u.employeeId, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY u.name ASC
        """)
    Page<UserEntity> search(@Param("search") String search, Pageable pageable);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(employee_id FROM 4) AS INTEGER)) FROM hr.users WHERE employee_id ~ '^EMP[0-9]+$'", nativeQuery = true)
    Integer findMaxEmployeeSequence();
}
