
package com.example.exam.repository;

import com.example.exam.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    // Get complete conversation between two users
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE (m.sender.id = :user1 AND m.receiver.id = :user2)
           OR (m.sender.id = :user2 AND m.receiver.id = :user1)
        ORDER BY m.timestamp ASC
    """)
    List<ChatMessage> findConversation(
            @Param("user1") Long user1,
            @Param("user2") Long user2
    );

    // Count unread messages from one user to another
    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.sender.id = :senderId
          AND m.receiver.id = :receiverId
          AND m.isRead = false
    """)
    long countUnreadMessages(
            @Param("senderId") Long senderId,
            @Param("receiverId") Long receiverId
    );

    // Mark messages as read
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ChatMessage m
        SET m.isRead = true
        WHERE m.sender.id = :senderId
          AND m.receiver.id = :receiverId
          AND m.isRead = false
    """)
    int markMessagesAsRead(
            @Param("senderId") Long senderId,
            @Param("receiverId") Long receiverId
    );
}
