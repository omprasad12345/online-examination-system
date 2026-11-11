
package com.example.exam.dto;

import com.example.exam.model.ChatMessage;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatMessageResponse {

    private Long id;

    private Long senderId;

    private String senderName;

    private Long receiverId;

    private String receiverName;

    private String content;

    private LocalDateTime timestamp;

    private boolean isRead;

    public static ChatMessageResponse from(ChatMessage message) {

        ChatMessageResponse response = new ChatMessageResponse();

        response.setId(message.getId());

        response.setSenderId(message.getSender().getId());
        response.setSenderName(message.getSender().getFullName());

        response.setReceiverId(message.getReceiver().getId());
        response.setReceiverName(message.getReceiver().getFullName());

        response.setContent(message.getContent());
        response.setTimestamp(message.getTimestamp());
        response.setRead(message.isRead());

        return response;
    }
}

