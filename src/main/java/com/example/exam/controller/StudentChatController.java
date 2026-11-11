
package com.example.exam.controller;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class StudentChatController {

    private final UserRepository userRepository;

    public StudentChatController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/chat")
    public String studentChat(
            Model model,
            Authentication authentication) {

        User currentUser = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        User admin = userRepository
                .findByRole("ROLE_ADMIN")
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Admin not found"));

        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("adminId", admin.getId());
        model.addAttribute("adminName", admin.getFullName());

        return "student/chat";
    }
}

