package com.chatapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User entity representing a chat participant.
 *
 * Maps to the 'users' table in MySQL.
 * Tracks online presence and session information.
 *
 * @author ChatApp Team
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username", unique = true),
    @Index(name = "idx_users_online", columnList = "is_online")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sentMessages", "receivedMessages"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Unique username used for identification and private messaging routing.
     * This is the principal name used in Spring WebSocket user destinations.
     */
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Display name shown in the chat UI (can differ from username).
     */
    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * WebSocket session ID - updated on connect/disconnect.
     * Used to track active connections.
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Online status - toggled by WebSocket connect/disconnect events.
     */
    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private boolean online = false;

    /**
     * URL path to user's avatar image (optional).
     */
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    /**
     * Timestamp of user's last activity.
     */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Relationships ───────────────────────────────────────────────────────

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Message> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Message> receivedMessages = new ArrayList<>();

    // ─── Helper Methods ──────────────────────────────────────────────────────

    /**
     * Returns display name if set, otherwise falls back to username.
     */
    public String getEffectiveDisplayName() {
        return (displayName != null && !displayName.isBlank()) ? displayName : username;
    }
}
