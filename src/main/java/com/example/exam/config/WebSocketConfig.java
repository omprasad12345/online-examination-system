
package com.example.exam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Configure message broker
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        // Broker sends messages to subscribed users
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages sent from frontend to backend
        config.setApplicationDestinationPrefixes("/app");
    }

    // Register WebSocket connection endpoint
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws");
    }
}

