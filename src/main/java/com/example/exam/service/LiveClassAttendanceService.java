package com.example.exam.service;

import com.example.exam.model.LiveClass;
import com.example.exam.model.LiveClassAttendance;
import com.example.exam.model.User;
import com.example.exam.repository.LiveClassAttendanceRepository;
import com.example.exam.repository.LiveClassRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LiveClassAttendanceService {

    private final LiveClassAttendanceRepository attendanceRepository;

    private final LiveClassRepository liveClassRepository;


    // ==========================================
    // 1. STUDENT JOINS CLASS
    // ==========================================

    public LiveClassAttendance studentJoined(Long classId, User student) {

        LiveClass liveClass = liveClassRepository.findById(classId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Live class not found"));

        if (!"LIVE".equals(liveClass.getStatus())) {
            throw new IllegalStateException(
                    "This class is not currently live"
            );
        }

        if (!"ROLE_STUDENT".equals(student.getRole())) {
            throw new IllegalArgumentException(
                    "Only students can register attendance"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        LiveClassAttendance attendance = attendanceRepository
                .findByLiveClass_IdAndStudent_Id(classId, student.getId())
                .orElse(null);

        if (attendance == null) {

            attendance = LiveClassAttendance.builder()
                    .liveClass(liveClass)
                    .student(student)
                    .joinedAt(now)
                    .lastJoinedAt(now)
                    .lastHeartbeatAt(now)
                    .leftAt(null)
                    .disconnectedAt(null)
                    .totalDurationSeconds(0L)
                    .currentlyJoined(true)
                    .connectionLost(false)
                    .build();

        } else {

            if (Boolean.TRUE.equals(attendance.getCurrentlyJoined())) {
                return attendance;
            }

            attendance.setLastJoinedAt(now);
            attendance.setLastHeartbeatAt(now);
            attendance.setLeftAt(null);
            attendance.setDisconnectedAt(null);
            attendance.setCurrentlyJoined(true);
            attendance.setConnectionLost(false);
        }

        return attendanceRepository.save(attendance);
    }


    // ==========================================
    // 2. STUDENT LEAVES CLASS
    // ==========================================

    public LiveClassAttendance studentLeft(Long classId, User student) {

        LiveClassAttendance attendance = attendanceRepository
                .findByLiveClass_IdAndStudent_Id(classId, student.getId())
                .orElseThrow(() ->
                        new IllegalStateException("Attendance record not found"));

        if (!Boolean.TRUE.equals(attendance.getCurrentlyJoined())) {
            return attendance;
        }

        LocalDateTime now = LocalDateTime.now();

        // Do not count the disconnected period
        if (!Boolean.TRUE.equals(attendance.getConnectionLost())
                && attendance.getLastJoinedAt() != null) {

            long duration = Duration.between(
                    attendance.getLastJoinedAt(),
                    now
            ).getSeconds();

            attendance.setTotalDurationSeconds(
                    attendance.getTotalDurationSeconds()
                            + Math.max(0, duration)
            );
        }

        attendance.setLeftAt(now);
        attendance.setCurrentlyJoined(false);
        attendance.setConnectionLost(false);
        attendance.setDisconnectedAt(null);

        return attendanceRepository.save(attendance);
    }


    // ==========================================
    // 3. STUDENT HEARTBEAT
    // ==========================================

    public void studentHeartbeat(Long classId, User student) {

        LiveClassAttendance attendance = attendanceRepository
                .findByLiveClass_IdAndStudent_Id(classId, student.getId())
                .orElseThrow(() ->
                        new IllegalStateException("Attendance record not found"));

        if (!Boolean.TRUE.equals(attendance.getCurrentlyJoined())) {
            return;
        }

        if (Boolean.TRUE.equals(attendance.getConnectionLost())) {
            return;
        }

        attendance.setLastHeartbeatAt(LocalDateTime.now());

        attendanceRepository.save(attendance);
    }


    // ==========================================
    // 4. STUDENT DISCONNECTS
    // ==========================================

    public void studentDisconnected(Long classId, User student) {

        LiveClassAttendance attendance = attendanceRepository
                .findByLiveClass_IdAndStudent_Id(classId, student.getId())
                .orElseThrow(() ->
                        new IllegalStateException("Attendance record not found"));

        if (!Boolean.TRUE.equals(attendance.getCurrentlyJoined())
                || Boolean.TRUE.equals(attendance.getConnectionLost())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Save only the connected portion of this session
        if (attendance.getLastJoinedAt() != null) {

            long duration = Duration.between(
                    attendance.getLastJoinedAt(),
                    now
            ).getSeconds();

            attendance.setTotalDurationSeconds(
                    attendance.getTotalDurationSeconds()
                            + Math.max(0, duration)
            );
        }

        attendance.setConnectionLost(true);
        attendance.setDisconnectedAt(now);

        attendanceRepository.save(attendance);
    }


    // ==========================================
    // 5. STUDENT RECONNECTS
    // ==========================================

    public void studentReconnected(Long classId, User student) {

        LiveClassAttendance attendance = attendanceRepository
                .findByLiveClass_IdAndStudent_Id(classId, student.getId())
                .orElseThrow(() ->
                        new IllegalStateException("Attendance record not found"));

        if (!Boolean.TRUE.equals(attendance.getCurrentlyJoined())
                || !Boolean.TRUE.equals(attendance.getConnectionLost())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Verify that the class is still live
        LiveClass liveClass = attendance.getLiveClass();

        if (!"LIVE".equals(liveClass.getStatus())) {
            throw new IllegalStateException(
                    "This class is no longer live"
            );
        }

        // Start a new connected session
        attendance.setLastJoinedAt(now);
        attendance.setLastHeartbeatAt(now);
        attendance.setConnectionLost(false);
        attendance.setDisconnectedAt(null);

        attendanceRepository.save(attendance);
    }


    // ==========================================
    // 6. GET CLASS ATTENDANCE
    // ==========================================

    @Transactional(readOnly = true)
    public List<LiveClassAttendance> getClassAttendance(Long classId) {

        return attendanceRepository
                .findByLiveClass_IdOrderByJoinedAtAsc(classId);
    }


    // ==========================================
    // 7. GET STUDENT ATTENDANCE
    // ==========================================

    @Transactional(readOnly = true)
    public List<LiveClassAttendance> getStudentAttendance(Long studentId) {

        return attendanceRepository
                .findByStudent_IdOrderByJoinedAtDesc(studentId);
    }

// ==========================================
// 8. AUTOMATIC DISCONNECT RECOVERY
// ==========================================

    public void processDisconnectedStudents() {

        LocalDateTime now = LocalDateTime.now();

        // Step 1: Detect students whose heartbeat is older than 45 seconds
        LocalDateTime heartbeatCutoff = now.minusSeconds(45);

        List<LiveClassAttendance> staleRecords =
                attendanceRepository.findStaleAttendance(heartbeatCutoff);

        for (LiveClassAttendance attendance : staleRecords) {

            if (!Boolean.TRUE.equals(attendance.getCurrentlyJoined())
                    || Boolean.TRUE.equals(attendance.getConnectionLost())) {
                continue;
            }

            attendance.setConnectionLost(true);
            attendance.setDisconnectedAt(now);

            attendanceRepository.save(attendance);
        }

        // Step 2: Close sessions disconnected for more than 2 minutes
        List<LiveClassAttendance> allActiveRecords =
                attendanceRepository.findByCurrentlyJoinedTrue();

        LocalDateTime disconnectCutoff = now.minusMinutes(2);

        for (LiveClassAttendance attendance : allActiveRecords) {

            if (!Boolean.TRUE.equals(attendance.getConnectionLost())) {
                continue;
            }

            LocalDateTime disconnectedAt = attendance.getDisconnectedAt();

            if (disconnectedAt == null
                    || disconnectedAt.isAfter(disconnectCutoff)) {
                continue;
            }

            attendance.setLeftAt(disconnectedAt);
            attendance.setCurrentlyJoined(false);
            attendance.setConnectionLost(false);
            attendance.setDisconnectedAt(null);
            attendance.setLastHeartbeatAt(null);

            attendanceRepository.save(attendance);
        }
    }


}
