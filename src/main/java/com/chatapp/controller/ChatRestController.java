package com.chatapp.controller;

import com.chatapp.dto.ChatDTOs.*;
import com.chatapp.entity.User;
import com.chatapp.exception.ChatExceptions.*;
import com.chatapp.service.ImageService;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for the Chat Application.
 *
 * Exposes HTTP endpoints for:
 * - User registration and online status
 * - Message history retrieval (for initial page load)
 * - Image upload (via HTTP multipart, returns URL used in WebSocket message)
 * - Health and status endpoints
 *
 * WebSocket communication is handled in ChatWebSocketController.
 * REST is used for setup operations and large file transfers (images).
 *
 * @author ChatApp Team
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatRestController {

    private final UserService userService;
    private final MessageService messageService;
    private final ImageService imageService;

    // ─── User Endpoints ───────────────────────────────────────────────────────

    /**
     * POST /api/users/join
     * Registers a new user or retrieves an existing one.
     * Called by the frontend login form before establishing WebSocket.
     */
    @PostMapping("/users/join")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinChat(
            @Valid @RequestBody UserJoinRequest request) {
        log.info("Join request for username: {}", request.getUsername());

        User user = userService.registerOrGetUser(request);

        Map<String, Object> responseData = Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getEffectiveDisplayName(),
                "message", "Welcome to the chat, " + user.getEffectiveDisplayName() + "!"
        );

        log.info("User {} joined successfully (id={})", user.getUsername(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Login successful", responseData));
    }

    /**
     * GET /api/users/online
     * Returns list of all currently online users.
     * Used to populate the sidebar on initial load.
     */
    @GetMapping("/users/online")
    public ResponseEntity<ApiResponse<List<OnlineUserDTO>>> getOnlineUsers() {
        List<OnlineUserDTO> users = userService.getOnlineUsers();
        log.debug("Online users requested: {} online", users.size());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * GET /api/users/{username}/exists
     * Checks if a username is already taken.
     * Used for live validation during login.
     */
    @GetMapping("/users/{username}/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkUserExists(@PathVariable String username) {
        try {
            userService.findByUsername(username);
            return ResponseEntity.ok(ApiResponse.success(true));
        } catch (UserNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    // ─── Message History Endpoints ────────────────────────────────────────────

    /**
     * GET /api/messages/group
     * Retrieves group chat message history.
     * Called on page load to populate the chat window with recent messages.
     *
     * @param limit  Max number of messages to return (default 50, max 200)
     */
    @GetMapping("/messages/group")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getGroupHistory(
            @RequestParam(defaultValue = "50") int limit) {

        if (limit > 200) limit = 200; // Safety cap
        log.debug("Group chat history requested: limit={}", limit);

        List<MessageResponse> messages = messageService.getGroupChatHistory(limit);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * GET /api/messages/private/{user1}/{user2}
     * Retrieves private conversation history between two users.
     * Called when a private chat window is opened.
     *
     * @param user1  First user's username
     * @param user2  Second user's username
     * @param limit  Max number of messages (default 50)
     */
    @GetMapping("/messages/private/{user1}/{user2}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getPrivateHistory(
            @PathVariable String user1,
            @PathVariable String user2,
            @RequestParam(defaultValue = "50") int limit) {

        log.debug("Private history requested: {} <-> {}", user1, user2);
        List<MessageResponse> messages = messageService.getPrivateConversationHistory(user1, user2, limit);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * POST /api/messages/{messageId}/read
     * Marks a private conversation as read.
     */
    @PostMapping("/messages/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @RequestParam String readerUsername,
            @RequestParam String otherUsername) {

        messageService.markConversationAsRead(readerUsername, otherUsername);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    // ─── Image Upload Endpoint ────────────────────────────────────────────────

    /**
     * POST /api/images/upload
     * Handles image file upload via HTTP multipart.
     *
     * Why use HTTP instead of WebSocket for images?
     * - WebSocket messages have message size limits
     * - HTTP multipart is designed for large binary transfers
     * - Allows progress indication
     * - Better error handling
     *
     * Workflow:
     * 1. Client uploads image via this REST endpoint
     * 2. Server saves image, returns URL
     * 3. Client sends WebSocket message with imageUrl = returned URL
     * 4. Other clients load image from /uploads/** static path
     *
     * @param file          The image file from multipart form data
     * @param senderUsername The uploader's username
     * @param receiverUsername  Target user (null for group)
     * @param isGroupMessage   true for group, false for private
     */
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senderUsername") String senderUsername,
            @RequestParam(value = "receiverUsername", required = false) String receiverUsername,
            @RequestParam(value = "isGroupMessage", defaultValue = "true") boolean isGroupMessage,
            @RequestParam(value = "caption", required = false) String caption) {

        log.info("Image upload from: {} (file: {}, size: {} bytes, type: {})",
                senderUsername, file.getOriginalFilename(), file.getSize(), file.getContentType());

        // Save image and get URL
        String imageUrl = imageService.saveImage(file);

        // Build the WebSocket message request
        var messageRequest = MessageResponse.builder()
                .senderUsername(senderUsername)
                .receiverUsername(receiverUsername)
                .messageType(com.chatapp.entity.Message.MessageType.IMAGE)
                .imageUrl(imageUrl)
                .imageContentType(file.getContentType())
                .content(caption)
                .groupMessage(isGroupMessage)
                .build();

        // Auto-send the image message via WebSocket broadcast
        if (isGroupMessage) {
            var sendRequest = com.chatapp.dto.ChatDTOs.SendMessageRequest.builder()
                    .senderUsername(senderUsername)
                    .content(caption)
                    .messageType(com.chatapp.entity.Message.MessageType.IMAGE)
                    .groupMessage(true)
                    .imageContentType(file.getContentType())
                    .build();
            var persistedMsg = messageService.sendGroupMessage(sendRequest);
            // Update image URL on the persisted message
            // (In production: update entity directly; this is simplified)
        }

        Map<String, Object> result = Map.of(
                "imageUrl", imageUrl,
                "originalName", file.getOriginalFilename(),
                "contentType", file.getContentType(),
                "sizeBytes", file.getSize()
        );

        log.info("Image uploaded successfully: {}", imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", result));
    }

    // ─── Health / Info Endpoints ──────────────────────────────────────────────

    /**
     * GET /api/status
     * Returns application status and statistics.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        Map<String, Object> status = Map.of(
                "application", "RealTime Chat",
                "version", "1.0.0",
                "onlineUsers", userService.getOnlineUserCount(),
                "status", "UP"
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
