
package com.example.exam.controller;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;
import com.example.exam.service.LiveClassAttendanceService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/student/live-classes")
@RequiredArgsConstructor
public class LiveClassAttendanceController {

    private final LiveClassAttendanceService attendanceService;
    private final UserRepository userRepository;

    // Record student joining a live class
    @PostMapping("/{classId}/attendance/join")
    @ResponseBody
    public ResponseEntity<String> studentJoined(
            @PathVariable Long classId,
            Authentication authentication) {

        User student = getAuthenticatedStudent(authentication);

        attendanceService.studentJoined(classId, student);

        return ResponseEntity.ok("Attendance started successfully");
    }

    // Record student leaving a live class
    @PostMapping("/{classId}/attendance/leave")
    @ResponseBody
    public ResponseEntity<String> studentLeft(
            @PathVariable Long classId,
            Authentication authentication) {

        User student = getAuthenticatedStudent(authentication);

        attendanceService.studentLeft(classId, student);

        return ResponseEntity.ok("Attendance updated successfully");
    }

    // Validate the authenticated student
    private User getAuthenticatedStudent(Authentication authentication) {

        User student = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found"));

        if (!"ROLE_STUDENT".equals(student.getRole())) {
            throw new AccessDeniedException(
                    "Only students can record attendance");
        }

        return student;
    }
}

