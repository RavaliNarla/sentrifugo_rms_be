package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.UserEntity;
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
    List<UserEntity> findByRoleIn(List<String> roles);
    List<UserEntity> findAllByOrderByNameAsc();

    @Query("""
        SELECT u FROM UserEntity u
        WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.role) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY u.name ASC
        """)
    List<UserEntity> search(@Param("search") String search);
}
