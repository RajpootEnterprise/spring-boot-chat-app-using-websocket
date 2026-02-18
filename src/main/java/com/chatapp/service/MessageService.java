package com.chatapp.service;

import com.chatapp.dto.ChatDTOs.*;
import com.chatapp.entity.Message;
import com.chatapp.entity.User;
import com.chatapp.exception.ChatExceptions.*;
import com.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for all chat message operations.
 *
 * Handles:
 * - Processing and persisting group + private messages
 * - WebSocket message routing via SimpMessagingTemplate
 * - Message history retrieval
 * - Read receipt management
 * - DTO mapping (Entity → MessageResponse)
 *
 * Routing architecture:
 * - Group messages: broadcast to /topic/group-chat
 * - Private messages: routed to /user/{receiverUsername}/queue/private
 *
 * @author ChatApp Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int DEFAULT_HISTORY_LIMIT = 50;

    // ─── Send Group Message ───────────────────────────────────────────────────

    /**
     * Processes and broadcasts a group chat message.
     *
     * Flow:
     * 1. Validate sender exists
     * 2. Build and persist Message entity
     * 3. Map to response DTO
     * 4. Broadcast to /topic/group-chat (all connected clients receive it)
     *
     * @param request SendMessageRequest from WebSocket client
     * @return Persisted MessageResponse
     */
    @Transactional
    public MessageResponse sendGroupMessage(SendMessageRequest request) {
        log.debug("Processing group message from: {}", request.getSenderUsername());

        // Validate sender
        User sender = userService.findByUsername(request.getSenderUsername());

        // Validate content for text messages
        if (Message.MessageType.TEXT.equals(request.getMessageType())
                && (request.getContent() == null || request.getContent().isBlank())) {
            throw new InvalidMessageException("Text message content cannot be empty");
        }

        // Build and persist message
        Message message = Message.builder()
                .content(request.getContent())
                .messageType(request.getMessageType())
                .sender(sender)
                .groupMessage(true)
                .imageContentType(request.getImageContentType())
                .build();

        Message saved = messageRepository.save(message);
        log.info("Group message persisted (id={}) from user: {}", saved.getId(), sender.getUsername());

        // Map to response DTO
        MessageResponse response = toMessageResponse(saved);

        // Attach base64 image data if present (for WebSocket delivery)
        if (request.getImageBase64() != null) {
            response.setImageBase64(request.getImageBase64());
        }

        // Broadcast to all subscribers on /topic/group-chat
        messagingTemplate.convertAndSend("/topic/group-chat", response);
        log.debug("Group message broadcast to /topic/group-chat");

        return response;
    }

    // ─── Send Private Message ─────────────────────────────────────────────────

    /**
     * Processes and routes a private message to the intended recipient.
     *
     * Flow:
     * 1. Validate sender and receiver exist
     * 2. Build conversation ID
     * 3. Persist message
     * 4. Send to receiver via /user/{username}/queue/private
     * 5. Send echo back to sender's own queue (so they see their sent message)
     *
     * @param request SendMessageRequest with receiverUsername set
     * @return Persisted MessageResponse
     */
    @Transactional
    public MessageResponse sendPrivateMessage(SendMessageRequest request) {
        log.debug("Processing private message from: {} to: {}",
                request.getSenderUsername(), request.getReceiverUsername());

        if (request.getReceiverUsername() == null || request.getReceiverUsername().isBlank()) {
            throw new InvalidMessageException("Receiver username is required for private messages");
        }

        // Validate both users
        User sender = userService.findByUsername(request.getSenderUsername());
        User receiver = userService.findByUsername(request.getReceiverUsername());

        // Prevent self-messaging
        if (sender.getUsername().equals(receiver.getUsername())) {
            throw new InvalidMessageException("Cannot send message to yourself");
        }

        // Validate content
        if (Message.MessageType.TEXT.equals(request.getMessageType())
                && (request.getContent() == null || request.getContent().isBlank())) {
            throw new InvalidMessageException("Message content cannot be empty");
        }

        // Build conversation ID (consistent regardless of direction)
        String conversationId = Message.buildConversationId(sender.getId(), receiver.getId());

        // Build and persist message
        Message message = Message.builder()
                .content(request.getContent())
                .messageType(request.getMessageType())
                .sender(sender)
                .receiver(receiver)
                .conversationId(conversationId)
                .groupMessage(false)
                .imageContentType(request.getImageContentType())
                .read(false)
                .build();

        Message saved = messageRepository.save(message);
        log.info("Private message persisted (id={}) from {} to {}", saved.getId(),
                sender.getUsername(), receiver.getUsername());

        MessageResponse response = toMessageResponse(saved);
        if (request.getImageBase64() != null) {
            response.setImageBase64(request.getImageBase64());
        }

        // Deliver to receiver's personal queue
        // Spring resolves /user/{receiverUsername}/queue/private automatically
        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/private",
                response
        );
        log.debug("Private message sent to /user/{}/queue/private", receiver.getUsername());

        // Echo back to sender so they see the message in their own chat window
        messagingTemplate.convertAndSendToUser(
                sender.getUsername(),
                "/queue/private",
                response
        );

        return response;
    }

    // ─── System Messages ──────────────────────────────────────────────────────

    /**
     * Broadcasts a system notification (user joined/left, etc.) to the group chat.
     * System messages are not persisted to reduce noise in message history.
     *
     * @param content  System message text
     */
    public void broadcastSystemMessage(String content) {
        MessageResponse systemMsg = MessageResponse.builder()
                .content(content)
                .messageType(Message.MessageType.SYSTEM)
                .groupMessage(true)
                .senderUsername("SYSTEM")
                .senderDisplayName("System")
                .build();

        messagingTemplate.convertAndSend("/topic/group-chat", systemMsg);
        log.debug("System message broadcast: {}", content);
    }

    // ─── History Retrieval ────────────────────────────────────────────────────

    /**
     * Retrieves the last N group chat messages for initial load on connect.
     * Returns messages in chronological order (oldest first).
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getGroupChatHistory(int limit) {
        log.debug("Fetching last {} group chat messages", limit);
        List<Message> messages = messageRepository.findLastGroupMessages(limit);

        // findLastGroupMessages returns newest first, so reverse for chronological order
        List<Message> chronological = messages.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());

        return chronological.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves conversation history between two users.
     * Used when opening a private chat window.
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getPrivateConversationHistory(String user1, String user2, int limit) {
        User u1 = userService.findByUsername(user1);
        User u2 = userService.findByUsername(user2);

        String conversationId = Message.buildConversationId(u1.getId(), u2.getId());
        log.debug("Fetching conversation history: {}", conversationId);

        PageRequest pageable = PageRequest.of(0, limit, Sort.by("createdAt").ascending());
        List<Message> messages = messageRepository.findByConversationId(conversationId, pageable);

        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Default history retrieval with default limit.
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getGroupChatHistory() {
        return getGroupChatHistory(DEFAULT_HISTORY_LIMIT);
    }

    // ─── Read Receipts ────────────────────────────────────────────────────────

    /**
     * Marks all messages in a conversation as read for the given user.
     */
    @Transactional
    public void markConversationAsRead(String readerUsername, String otherUsername) {
        User reader = userService.findByUsername(readerUsername);
        User other = userService.findByUsername(otherUsername);
        String conversationId = Message.buildConversationId(reader.getId(), other.getId());

        int count = messageRepository.markConversationAsRead(conversationId, readerUsername);
        log.debug("Marked {} messages as read in conversation {} for {}", count, conversationId, readerUsername);
    }

    /**
     * Gets unread message count for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return messageRepository.countUnreadMessages(username);
    }

    // ─── DTO Mapping ─────────────────────────────────────────────────────────

    /**
     * Maps a Message entity to a MessageResponse DTO.
     * Called before sending to WebSocket clients or REST responses.
     */
    public MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderUsername(message.getSender().getUsername())
                .senderDisplayName(message.getSender().getEffectiveDisplayName())
                .receiverUsername(message.getReceiver() != null
                        ? message.getReceiver().getUsername() : null)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .imageUrl(message.getImageUrl())
                .imageContentType(message.getImageContentType())
                .groupMessage(message.isGroupMessage())
                .timestamp(message.getCreatedAt())
                .read(message.isRead())
                .build();
    }
}
