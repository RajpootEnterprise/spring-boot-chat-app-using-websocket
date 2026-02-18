package com.chatapp.service;

import com.chatapp.dto.ChatDTOs.*;
import com.chatapp.entity.User;
import com.chatapp.exception.ChatExceptions.*;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class handling all user-related business logic.
 *
 * Responsibilities:
 * - User registration and lookup
 * - Online presence management (connect/disconnect tracking)
 * - Building online user lists for the chat sidebar
 *
 * All database interactions go through UserRepository.
 * No direct SQL queries in this layer.
 *
 * @author ChatApp Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ─── User Registration / Login ───────────────────────────────────────────

    /**
     * Registers a new user or retrieves an existing one.
     * If username exists: returns existing user (allows re-joining).
     * If username doesn't exist: creates and persists a new user.
     *
     * This is a simplified authentication model. In production,
     * use Spring Security with password hashing.
     *
     * @param request UserJoinRequest with username and optional displayName
     * @return The User entity (new or existing)
     */
    @Transactional
    public User registerOrGetUser(UserJoinRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        log.info("Register/get user request for username: {}", username);

        return userRepository.findByUsernameIgnoreCase(username)
                .map(existingUser -> {
                    // Update display name if provided
                    if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
                        existingUser.setDisplayName(request.getDisplayName().trim());
                        userRepository.save(existingUser);
                        log.debug("Updated display name for user: {}", username);
                    }
                    log.info("Returning existing user: {}", username);
                    return existingUser;
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(username)
                            .displayName(request.getDisplayName() != null
                                    ? request.getDisplayName().trim()
                                    : request.getUsername().trim())
                            .online(false)
                            .build();
                    User saved = userRepository.save(newUser);
                    log.info("Created new user: {} (id={})", username, saved.getId());
                    return saved;
                });
    }

    /**
     * Find a user by username. Throws UserNotFoundException if not found.
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    /**
     * Find a user by username safely (returns null instead of throwing).
     */
    @Transactional(readOnly = true)
    public User findByUsernameOrNull(String username) {
        if (username == null || username.isBlank()) return null;
        return userRepository.findByUsernameIgnoreCase(username.trim()).orElse(null);
    }

    // ─── Online Presence Management ──────────────────────────────────────────

    /**
     * Marks a user as online when they connect via WebSocket.
     * Updates their session ID for future disconnect tracking.
     *
     * @param username  The connecting user's username
     * @param sessionId The WebSocket session ID assigned by Spring
     */
    @Transactional
    public User markUserOnline(String username, String sessionId) {
        log.info("Marking user ONLINE: {} (session: {})", username, sessionId);

        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseGet(() -> {
                    // Auto-create user if they don't exist (e.g., first-time connection)
                    User newUser = User.builder()
                            .username(username.toLowerCase())
                            .displayName(username)
                            .online(true)
                            .sessionId(sessionId)
                            .lastSeen(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });

        user.setOnline(true);
        user.setSessionId(sessionId);
        user.setLastSeen(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.debug("User {} is now online", username);
        return saved;
    }

    /**
     * Marks a user as offline when their WebSocket session closes.
     * Looks up user by session ID since disconnect events don't always carry username.
     *
     * @param sessionId The WebSocket session ID that disconnected
     */
    @Transactional
    public User markUserOfflineBySessionId(String sessionId) {
        log.info("Marking session offline: {}", sessionId);

        return userRepository.findBySessionId(sessionId)
                .map(user -> {
                    user.setOnline(false);
                    user.setSessionId(null);
                    user.setLastSeen(LocalDateTime.now());
                    User saved = userRepository.save(user);
                    log.info("User {} marked as OFFLINE", user.getUsername());
                    return saved;
                })
                .orElseGet(() -> {
                    log.warn("No user found for session ID: {}. Already disconnected?", sessionId);
                    return null;
                });
    }

    /**
     * Marks a user as offline by username.
     */
    @Transactional
    public void markUserOfflineByUsername(String username) {
        userRepository.findByUsernameIgnoreCase(username)
                .ifPresent(user -> {
                    user.setOnline(false);
                    user.setSessionId(null);
                    user.setLastSeen(LocalDateTime.now());
                    userRepository.save(user);
                    log.info("User {} marked as OFFLINE by username", username);
                });
    }

    // ─── Online Users List ───────────────────────────────────────────────────

    /**
     * Returns list of all currently online users.
     * Maps User entities to lightweight OnlineUserDTO objects.
     */
    @Transactional(readOnly = true)
    public List<OnlineUserDTO> getOnlineUsers() {
        return userRepository.findByOnlineTrueOrderByUsernameAsc()
                .stream()
                .map(this::toOnlineUserDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns count of online users.
     */
    @Transactional(readOnly = true)
    public long getOnlineUserCount() {
        return userRepository.countByOnlineTrue();
    }

    // ─── Reset Methods ───────────────────────────────────────────────────────

    /**
     * Called on application startup to reset stale online statuses
     * from a previous run (users who didn't properly disconnect).
     */
    @Transactional
    public void resetAllOnlineStatuses() {
        userRepository.markAllUsersOffline();
        log.info("All user online statuses reset on startup");
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private OnlineUserDTO toOnlineUserDTO(User user) {
        return OnlineUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getEffectiveDisplayName())
                .online(user.isOnline())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
