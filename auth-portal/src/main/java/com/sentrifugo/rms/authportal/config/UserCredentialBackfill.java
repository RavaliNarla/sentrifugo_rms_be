package com.sentrifugo.rms.authportal.config;

import com.sentrifugo.rms.common.util.AppConstants;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Backfill EMP#### codes and the default password for users created under Entra-only auth.
 * Also migrates away from reset_otp_hash → reset_otp.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCredentialBackfill implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE hr.users DROP COLUMN IF EXISTS reset_otp_hash");
        } catch (Exception e) {
            log.warn("Could not drop reset_otp_hash: {}", e.getMessage());
        }

        List<UserEntity> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(UserEntity::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Integer maxSeq = userRepository.findMaxEmployeeSequence();
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        String defaultHash = passwordEncoder.encode(AppConstants.DEFAULT_USER_PASSWORD);
        int updated = 0;

        for (UserEntity user : users) {
            boolean dirty = false;
            if (user.getEmployeeId() == null || user.getEmployeeId().isBlank()) {
                user.setEmployeeId(String.format("EMP%04d", nextSeq++));
                dirty = true;
            }
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                user.setPasswordHash(defaultHash);
                dirty = true;
            }
            if (dirty) {
                userRepository.save(user);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Backfilled employeeId/password for {} user(s). Default password: {}",
                    updated, AppConstants.DEFAULT_USER_PASSWORD);
        }
    }
}
