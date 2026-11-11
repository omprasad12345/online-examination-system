package com.example.exam.dto;

import com.example.exam.model.LiveClassAttendance;

import java.time.Duration;
import java.time.LocalDateTime;

public record LiveClassAttendanceReportDto(
        String studentName,
        String username,
        LocalDateTime joinedAt,
        LocalDateTime lastJoinedAt,
        LocalDateTime leftAt,
        long totalDurationSeconds,
        String formattedDuration,
        boolean currentlyJoined
) {

    public static LiveClassAttendanceReportDto from(
            LiveClassAttendance attendance) {

        long totalSeconds = attendance.getTotalDurationSeconds() == null
                ? 0L
                : attendance.getTotalDurationSeconds();

        // Include the current session if the student is still connected.
        if (Boolean.TRUE.equals(attendance.getCurrentlyJoined())
                && attendance.getLastJoinedAt() != null) {

            long currentSessionSeconds = Duration.between(
                    attendance.getLastJoinedAt(),
                    LocalDateTime.now()
            ).getSeconds();

            totalSeconds += Math.max(0, currentSessionSeconds);
        }

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String formattedDuration =
                hours + "h " + minutes + "m " + seconds + "s";

        return new LiveClassAttendanceReportDto(
                attendance.getStudent().getFullName(),
                attendance.getStudent().getUsername(),
                attendance.getJoinedAt(),
                attendance.getLastJoinedAt(),
                attendance.getLeftAt(),
                totalSeconds,
                formattedDuration,
                Boolean.TRUE.equals(attendance.getCurrentlyJoined())
        );
    }
}

