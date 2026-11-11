package com.example.exam.service;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiveClassAttendanceRecoveryScheduler {

    private final LiveClassAttendanceService attendanceService;

    // Runs every 30 seconds
    @Scheduled(fixedDelay = 30000)
    public void recoverDisconnectedStudents() {

        attendanceService.processDisconnectedStudents();
    }
}

