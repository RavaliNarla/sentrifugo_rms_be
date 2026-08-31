package com.bob.db.repository;

import com.bob.db.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
//    List<UserEntity> findAllByRole(String role);

    Optional<UserEntity> findByOathUserId(String oathUserId);

    boolean existsByEmailIgnoreCase(String email);

    List<UserEntity> findAllByIsActiveTrue();

//    List<UserEntity> findAllByUserIdInAndIsActiveTrue(List<Long> userIds);

    Optional<UserEntity> findFirstByEmailIgnoreCase(String email);

    List<UserEntity> findByRole(String interviewerRole);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    List<UserEntity> findByEmailIgnoreCaseIn(List<String> emails);

    List<UserEntity> findByRoleAndInterviewCenterId(String role,UUID interviewCentreId);

    List<UserEntity> findByRoleIn(List<String> roles);

    List<UserEntity> findByRoleAndInterviewCenterIdIn(String role,List<UUID> interviewCenterIds);


}