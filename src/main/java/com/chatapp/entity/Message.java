package com.chatapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Message entity representing a chat message (text or image).
 *
 * Supports both group messages (no receiver, no conversation ID)
 * and private messages (has receiver and conversation ID).
 *
 * Maps to the 'messages' table in MySQL.
 *
 * @author ChatApp Team
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_sender", columnList = "sender_id"),
    @Index(name = "idx_messages_receiver", columnList = "receiver_id"),
    @Index(name = "idx_messages_type", columnList = "message_type"),
    @Index(name = "idx_messages_timestamp", columnList = "created_at"),
    @Index(name = "idx_messages_conversation", columnList = "conversation_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The actual text content of the message.
     * For image messages, this can be a caption or empty.
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Type of message: TEXT, IMAGE, or SYSTEM.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    /**
     * The user who sent this message.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The receiver for private messages. Null for group messages.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    /**
     * For private chats: computed as sorted combo of sender+receiver IDs.
     * Enables efficient retrieval of conversation history.
     * Example: "user_3_user_7" (sorted by ID so it's consistent regardless of direction)
     */
    @Column(name = "conversation_id", length = 100)
    private String conversationId;

    /**
     * Whether this is a group message or private message.
     */
    @Column(name = "is_group_message", nullable = false)
    @Builder.Default
    private boolean groupMessage = false;

    /**
     * URL path to the associated image (for IMAGE type messages).
     * Relative path: /uploads/images/{filename}
     */
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    /**
     * Original filename of the uploaded image.
     */
    @Column(name = "image_original_name", length = 255)
    private String imageOriginalName;

    /**
     * MIME type of the image (e.g., image/jpeg, image/png).
     */
    @Column(name = "image_content_type", length = 50)
    private String imageContentType;

    /**
     * File size in bytes for the image.
     */
    @Column(name = "image_size_bytes")
    private Long imageSizeBytes;

    /**
     * Whether the receiver has read this message (for private messages).
     */
    @Column(name = "is_read")
    @Builder.Default
    private boolean read = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─── Enums ───────────────────────────────────────────────────────────────

    public enum MessageType {
        TEXT,       // Plain text message
        IMAGE,      // Image attachment (may include caption)
        SYSTEM      // System notification (user joined, left, etc.)
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    /**
     * Generates a consistent conversation ID from two user IDs.
     * Always produces the same ID regardless of who sent/received.
     */
    public static String buildConversationId(Long userId1, Long userId2) {
        long min = Math.min(userId1, userId2);
        long max = Math.max(userId1, userId2);
        return "user_" + min + "_user_" + max;
    }

    /**
     * Convenience check for private message.
     */
    public boolean isPrivateMessage() {
        return receiver != null;
    }
}
