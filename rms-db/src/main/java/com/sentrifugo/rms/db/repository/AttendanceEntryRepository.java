package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.AttendanceEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttendanceEntryRepository extends JpaRepository<AttendanceEntryEntity, UUID> {
}
