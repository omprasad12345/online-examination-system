
package com.example.exam.service;

import com.example.exam.model.ChatMessage;
import com.example.exam.model.User;
import com.example.exam.repository.ChatMessageRepository;

import com.example.exam.dto.ChatMessageResponse;
import java.util.stream.Collectors;

import com.example.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    // Constructor injection
    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    // Save a new chat message
    @Transactional
    public ChatMessage saveMessage(User sender, User receiver, String content) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        ChatMessage message = new ChatMessage();

        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content.trim());
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false);

        return chatMessageRepository.save(message);
    }

    // Retrieve previous conversation
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversation(Long user1, Long user2) {

        return chatMessageRepository.findConversation(user1, user2);
    }


    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversationHistory(
            Long user1, Long user2) {

        return chatMessageRepository
                .findConversation(user1, user2)
                .stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public long getUnreadCount(Long senderId, Long receiverId) {
        return chatMessageRepository.countUnreadMessages(
                senderId, receiverId
        );
    }

    @Transactional
    public int markMessagesAsRead(Long senderId, Long receiverId) {
        return chatMessageRepository.markMessagesAsRead(
                senderId,
                receiverId
        );
    }



}

