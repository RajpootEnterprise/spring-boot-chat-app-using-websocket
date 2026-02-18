package com.chatapp.controller;

import com.chatapp.dto.ChatDTOs.*;
import com.chatapp.entity.Message;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket Controller for real-time chat messaging.
 *
 * Handles:
 * - Group message broadcasting via /topic/group-chat
 * - Private message routing via /user/{username}/queue/private
 * - User join/leave events and presence broadcasting
 * - WebSocket session connect/disconnect lifecycle
 *
 * Message Flow:
 * ┌──────────┐  /app/chat.sendGroupMessage  ┌──────────────────────┐
 * │  Client  │ ─────────────────────────►  │ ChatWebSocketCtrl    │
 * └──────────┘                             └──────────┬───────────┘
 *                                                     │
 *                                     ┌───────────────▼──────────────┐
 *                                     │     MessageService           │
 *                                     │  - persist to DB             │
 *                                     │  - broadcast via             │
 *                                     │    /topic/group-chat         │
 *                                     └──────────────────────────────┘
 *
 * @author ChatApp Team
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Group Chat ───────────────────────────────────────────────────────────

    /**
     * Handles group chat messages sent to /app/chat.sendGroupMessage
     *
     * The message is:
     * 1. Persisted to MySQL
     * 2. Broadcast to all subscribers on /topic/group-chat
     *
     * Clients subscribe to /topic/group-chat to receive these messages.
     */
    @MessageMapping("/chat.sendGroupMessage")
    public void sendGroupMessage(@Valid @Payload SendMessageRequest request,
                                 SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.debug("Group message received from: {} (session: {})",
                request.getSenderUsername(), sessionId);

        // MessageService handles persistence + broadcasting
        MessageResponse response = messageService.sendGroupMessage(request);
        log.info("Group message processed: id={}, from={}", response.getId(), response.getSenderUsername());
    }

    // ─── Private Messaging ────────────────────────────────────────────────────

    /**
     * Handles private messages sent to /app/chat.sendPrivateMessage
     *
     * The message is:
     * 1. Persisted to MySQL
     * 2. Delivered to receiver's personal queue: /user/{receiver}/queue/private
     * 3. Echoed back to sender's personal queue for UI consistency
     *
     * Clients subscribe to /user/queue/private to receive these messages.
     * Spring automatically resolves /user/queue/private to
     * /user/{principal}/queue/private based on the user's session.
     */
    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Valid @Payload SendMessageRequest request,
                                   SimpMessageHeaderAccessor headerAccessor) {
        log.debug("Private message from: {} to: {}",
                request.getSenderUsername(), request.getReceiverUsername());

        // MessageService handles persistence + routing
        MessageResponse response = messageService.sendPrivateMessage(request);
        log.info("Private message processed: id={}, from={}, to={}",
                response.getId(), response.getSenderUsername(), response.getReceiverUsername());
    }

    // ─── User Join / Presence ─────────────────────────────────────────────────

    /**
     * Handles user joining the chat room.
     * Sent by client when they authenticate and enter the chat.
     *
     * Broadcasts a presence event to all connected users so their
     * online user lists get updated.
     */
    @MessageMapping("/chat.join")
    public void userJoined(@Payload UserJoinRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = request.getUsername().trim().toLowerCase();

        log.info("User joining chat: {} (session: {})", username, sessionId);

        // Mark user online in database
        userService.markUserOnline(username, sessionId);

        // Store username in WebSocket session attributes for disconnect tracking
        headerAccessor.getSessionAttributes().put("username", username);

        // Broadcast presence event to all connected clients
        UserPresenceEvent joinEvent = UserPresenceEvent.builder()
                .username(username)
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : username)
                .status(UserPresenceEvent.UserStatus.JOINED)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/presence", joinEvent);

        // Send updated online users list to all clients
        broadcastOnlineUsers();

        // Post system message to group chat
        messageService.broadcastSystemMessage(username + " joined the chat 👋");

        log.info("User {} joined successfully", username);
    }

    // ─── WebSocket Lifecycle Events ───────────────────────────────────────────

    /**
     * Listens for WebSocket session connect events (low-level).
     * Called when a WebSocket connection is first established,
     * before STOMP CONNECT frame processing.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.debug("WebSocket connection established: session={}",
                event.getMessage().getHeaders().get("simpSessionId"));
    }

    /**
     * Listens for WebSocket session disconnect events.
     * Called when a client's WebSocket connection closes (browser tab closed,
     * network disconnect, explicit disconnect, etc.)
     *
     * This is where we handle the "user left" logic.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("WebSocket disconnected: session={}", sessionId);

        // Find and mark user offline using session ID
        var disconnectedUser = userService.markUserOfflineBySessionId(sessionId);

        if (disconnectedUser != null) {
            String username = disconnectedUser.getUsername();
            log.info("User disconnected: {}", username);

            // Broadcast presence event: user left
            UserPresenceEvent leaveEvent = UserPresenceEvent.builder()
                    .username(username)
                    .displayName(disconnectedUser.getEffectiveDisplayName())
                    .status(UserPresenceEvent.UserStatus.LEFT)
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/presence", leaveEvent);

            // Send updated online users list
            broadcastOnlineUsers();

            // Post system message
            messageService.broadcastSystemMessage(username + " left the chat");
        }
    }

    // ─── Request-Response Endpoints ──────────────────────────────────────────

    /**
     * Allows clients to request their private message history.
     * Client sends to: /app/chat.getPrivateHistory
     * Response delivered to: /user/queue/private-history
     */
    @MessageMapping("/chat.getPrivateHistory")
    @SendToUser("/queue/private-history")
    public List<MessageResponse> getPrivateHistory(@Payload PrivateHistoryRequest request) {
        log.debug("Private history requested: {} <-> {}", request.getUser1(), request.getUser2());
        return messageService.getPrivateConversationHistory(
                request.getUser1(), request.getUser2(), 50);
    }

    /**
     * Inner class for private history request payload.
     */
    @lombok.Data
    public static class PrivateHistoryRequest {
        private String user1;
        private String user2;
    }

    // ─── Helper Methods ───────────────────────────────────────────────────────

    /**
     * Sends the current online users list to all connected clients.
     * Called after any connect/disconnect event.
     */
    private void broadcastOnlineUsers() {
        List<OnlineUserDTO> onlineUsers = userService.getOnlineUsers();
        messagingTemplate.convertAndSend("/topic/online-users", onlineUsers);
        log.debug("Online users broadcast: {} users", onlineUsers.size());
    }
}
