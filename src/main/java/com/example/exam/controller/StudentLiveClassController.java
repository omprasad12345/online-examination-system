
package com.example.exam.controller;

import com.example.exam.model.LiveClass;
import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;
import com.example.exam.service.LiveClassService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/student/live-classes")
@RequiredArgsConstructor
public class StudentLiveClassController {

    private final LiveClassService liveClassService;
    private final UserRepository userRepository;

    // Display all live classes to students
    @GetMapping
    public String viewLiveClasses(
            Authentication authentication,
            Model model) {

        User student = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        if (!"ROLE_STUDENT".equals(student.getRole())) {
            throw new AccessDeniedException(
                    "Only students can access live classes");
        }

        model.addAttribute(
                "liveClasses",
                liveClassService.getAllLiveClasses()
        );

        model.addAttribute("student", student);

        return "student/live-classes";
    }

    // Student joins a live class
    @GetMapping("/{id}/join")
    public String joinLiveClass(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        User student = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        if (!"ROLE_STUDENT".equals(student.getRole())) {
            throw new AccessDeniedException(
                    "Only students can join classes");
        }

        LiveClass liveClass =
                liveClassService.getLiveClassById(id);

        if (!"LIVE".equals(liveClass.getStatus())) {
            return "redirect:/student/live-classes";
        }

        model.addAttribute("classId", liveClass.getId());
        model.addAttribute("classTitle", liveClass.getTitle());
        model.addAttribute("roomName", liveClass.getRoomName());
        model.addAttribute("displayName", student.getFullName());
        model.addAttribute("isAdmin", false);

        return "live-class-room";
    }
}
