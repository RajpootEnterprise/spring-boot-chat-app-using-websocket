package com.chatapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket Configuration.
 * FIX: Added TaskScheduler bean — required when heartbeat values are configured.
 * Without it Spring throws: "Heartbeat values configured but no TaskScheduler provided"
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * REQUIRED for heartbeat support.
     * Must be explicitly passed to enableSimpleBroker().setTaskScheduler()
     */
    @Bean
    public TaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        log.info("WebSocket TaskScheduler initialized");
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{25000, 25000})
                .setTaskScheduler(webSocketTaskScheduler()); // <-- KEY FIX

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");

        log.info("WebSocket broker configured: /topic, /queue | app prefix: /app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(30000);

        log.info("STOMP endpoint registered at /ws-chat with SockJS fallback");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add auth interceptors here in production
    }
}