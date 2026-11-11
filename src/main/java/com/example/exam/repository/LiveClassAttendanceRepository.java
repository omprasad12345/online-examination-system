package com.example.exam.repository;

import com.example.exam.model.LiveClassAttendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LiveClassAttendanceRepository
        extends JpaRepository<LiveClassAttendance, Long> {

    // Find attendance for a particular student and class
    Optional<LiveClassAttendance> findByLiveClass_IdAndStudent_Id(
            Long liveClassId,
            Long studentId
    );

    // Get all attendance records for a class
    List<LiveClassAttendance> findByLiveClass_IdOrderByJoinedAtAsc(
            Long liveClassId
    );

    // Get all attendance records for a student
    List<LiveClassAttendance> findByStudent_IdOrderByJoinedAtDesc(
            Long studentId
    );

    // Find active attendance records
    List<LiveClassAttendance> findByCurrentlyJoinedTrue();

    // Find active attendance records with no recent heartbeat
    @Query("""
            SELECT a
            FROM LiveClassAttendance a
            WHERE a.currentlyJoined = true
              AND a.lastHeartbeatAt IS NOT NULL
              AND a.lastHeartbeatAt < :cutoffTime
            """)
    List<LiveClassAttendance> findStaleAttendance(
            @Param("cutoffTime") LocalDateTime cutoffTime
    );
}
