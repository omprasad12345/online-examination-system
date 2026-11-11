package com.example.exam.controller;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.Authentication;
import com.example.exam.service.ChatService;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminChatController {

    private final UserRepository userRepository;
    private final ChatService chatService;

    public AdminChatController(
            UserRepository userRepository,
            ChatService chatService) {

        this.userRepository = userRepository;
        this.chatService = chatService;
    }


    @GetMapping("/chat")
    public String adminChat(Model model, Authentication authentication) {

        List<User> students = userRepository.findByRole("ROLE_STUDENT");

        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Map<Long, Long> unreadCounts = new HashMap<>();

        for (User student : students) {
            long count = chatService.getUnreadCount(
                    student.getId(),
                    currentUser.getId()
            );

            unreadCounts.put(student.getId(), count);
        }

        model.addAttribute("students", students);
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("unreadCounts", unreadCounts);

        return "admin/chat";
    }
}