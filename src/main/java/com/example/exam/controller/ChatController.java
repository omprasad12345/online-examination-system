
package com.example.exam.controller;

import com.example.exam.dto.ChatMessageRequest;
import com.example.exam.dto.ChatMessageResponse;
import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;
import com.example.exam.service.ChatService;
import com.example.exam.dto.ChatReadReceipt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {

    private final ChatService chatService;

    private final UserRepository userRepository;

    private final SimpMessagingTemplate messagingTemplate;

    // Constructor injection
    public ChatController(
            ChatService chatService,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {

        this.chatService = chatService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // Receive message from Admin or Student
    @MessageMapping("/chat.send")
    public void sendMessage(
            @Payload ChatMessageRequest request,
            Principal principal) {

        // 1. Identify logged-in user
        if (principal == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        User sender = userRepository.findByUsername(principal.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Sender not found"));

        // 2. Validate message
        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("Receiver is required");
        }

        if (request.getContent() == null ||
                request.getContent().isBlank()) {

            throw new IllegalArgumentException("Message cannot be empty");
        }

        if (request.getContent().length() > 5000) {
            throw new IllegalArgumentException("Message is too long");
        }

        // 3. Find receiver
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Receiver not found"));

        // 4. Allow only Admin <-> Student communication
        String senderRole = sender.getRole();
        String receiverRole = receiver.getRole();

        boolean validRoles =
                ("ROLE_ADMIN".equals(senderRole) &&
                        "ROLE_STUDENT".equals(receiverRole))
                        ||
                        ("ROLE_STUDENT".equals(senderRole) &&
                                "ROLE_ADMIN".equals(receiverRole));

        if (!validRoles) {
            throw new AccessDeniedException(
                    "Only Admin and Student can communicate");
        }

        // 5. Save message in PostgreSQL
        var savedMessage = chatService.saveMessage(
                sender,
                receiver,
                request.getContent()
        );

        // 6. Convert entity into safe response DTO
        ChatMessageResponse response =
                ChatMessageResponse.from(savedMessage);

        // 7. Deliver message to receiver
        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/messages",
                response
        );

        // 8. Send a copy to sender for confirmation
        messagingTemplate.convertAndSendToUser(
                sender.getUsername(),
                "/queue/messages",
                response
        );
    }


    @GetMapping("/chat/history/{otherUserId}")
    @ResponseBody
    public List<ChatMessageResponse> getChatHistory(
            @PathVariable Long otherUserId,
            Principal principal) {

        // Identify logged-in user
        User currentUser = userRepository
                .findByUsername(principal.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Find the other user
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() ->
                        new RuntimeException("Receiver not found"));

        // Allow only Admin <-> Student conversation
        boolean validRoles =
                ("ROLE_ADMIN".equals(currentUser.getRole()) &&
                        "ROLE_STUDENT".equals(otherUser.getRole()))
                        ||
                        ("ROLE_STUDENT".equals(currentUser.getRole()) &&
                                "ROLE_ADMIN".equals(otherUser.getRole()));

        if (!validRoles) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot access this conversation");
        }

        // Retrieve conversation history
        return chatService.getConversationHistory(
                currentUser.getId(),
                otherUser.getId()
        );
    }

    @PutMapping("/chat/read/{otherUserId}")
    @ResponseBody
    public String markAsRead(
            @PathVariable Long otherUserId,
            Principal principal) {

        User currentUser = userRepository
                .findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        User otherUser = userRepository
                .findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean validRoles =
                ("ROLE_ADMIN".equals(currentUser.getRole())
                        && "ROLE_STUDENT".equals(otherUser.getRole()))
                        || ("ROLE_STUDENT".equals(currentUser.getRole())
                        && "ROLE_ADMIN".equals(otherUser.getRole()));

        if (!validRoles) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot access this conversation"
            );
        }

        chatService.markMessagesAsRead(
                otherUser.getId(),
                currentUser.getId()
        );

        return "Messages marked as read";
    }

    // Receive read receipt from Admin or Student
    @MessageMapping("/chat.read")
    public void sendReadReceipt(
            @Payload ChatReadReceipt request,
            Principal principal) {

        if (principal == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        // Identify the user who opened the conversation
        User reader = userRepository.findByUsername(principal.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Reader not found"));

        if (request.getSenderId() == null) {
            throw new IllegalArgumentException("Sender ID is required");
        }

        // Find original sender
        User originalSender = userRepository.findById(request.getSenderId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Sender not found"));

        // Validate Admin <-> Student communication
        boolean validRoles =
                ("ROLE_ADMIN".equals(reader.getRole())
                        && "ROLE_STUDENT".equals(originalSender.getRole()))
                        ||
                        ("ROLE_STUDENT".equals(reader.getRole())
                                && "ROLE_ADMIN".equals(originalSender.getRole()));

        if (!validRoles) {
            throw new AccessDeniedException(
                    "Invalid read receipt conversation");
        }

        // Mark original sender's messages as read
        chatService.markMessagesAsRead(
                originalSender.getId(),
                reader.getId()
        );

        // Notify original sender
        ChatReadReceipt receipt = new ChatReadReceipt(
                reader.getId(),
                originalSender.getId()
        );

        messagingTemplate.convertAndSendToUser(
                originalSender.getUsername(),
                "/queue/read-receipts",
                receipt
        );
    }

}

