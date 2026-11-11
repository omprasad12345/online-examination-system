
package com.example.exam.service;

import com.example.exam.dto.LiveClassNotification;
import com.example.exam.model.LiveClass;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiveClassNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyClassStatus(LiveClass liveClass) {

        LiveClassNotification notification =
                new LiveClassNotification(
                        liveClass.getId(),
                        liveClass.getTitle(),
                        liveClass.getStatus()
                );

        messagingTemplate.convertAndSend(
                "/topic/live-classes",
                notification
        );
    }
}
