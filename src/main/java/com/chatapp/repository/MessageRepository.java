package com.chatapp.repository;

import com.chatapp.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Message entity.
 *
 * Provides queries for:
 * - Group chat history retrieval
 * - Private conversation history
 * - Unread message counts
 * - Image message lookup
 * - Message pagination for infinite scroll
 *
 * @author ChatApp Team
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ─── Group Messages ──────────────────────────────────────────────────────

    /**
     * Retrieve recent group messages ordered by timestamp ascending.
     * Used to load group chat history on connect.
     */
    @Query("SELECT m FROM Message m WHERE m.groupMessage = true " +
           "ORDER BY m.createdAt ASC")
    List<Message> findGroupMessages(Pageable pageable);

    /**
     * Retrieve group messages after a specific timestamp.
     * Used for lazy loading / pagination.
     */
    @Query("SELECT m FROM Message m WHERE m.groupMessage = true " +
           "AND m.createdAt > :after ORDER BY m.createdAt ASC")
    List<Message> findGroupMessagesAfter(@Param("after") LocalDateTime after);

    // ─── Private Messages ────────────────────────────────────────────────────

    /**
     * Retrieve conversation between two users using the conversation ID.
     * ConversationId is a sorted combination of both user IDs.
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "ORDER BY m.createdAt ASC")
    List<Message> findByConversationId(@Param("conversationId") String conversationId,
                                       Pageable pageable);

    /**
     * Count unread private messages for a specific receiver.
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.username = :username " +
           "AND m.read = false AND m.groupMessage = false")
    long countUnreadMessages(@Param("username") String username);

    /**
     * Count unread messages in a specific private conversation.
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.receiver.username = :username AND m.read = false")
    long countUnreadInConversation(@Param("conversationId") String conversationId,
                                   @Param("username") String username);

    /**
     * Mark all messages in a conversation as read for a specific user.
     */
    @Modifying
    @Query("UPDATE Message m SET m.read = true WHERE m.conversationId = :conversationId " +
           "AND m.receiver.username = :username AND m.read = false")
    int markConversationAsRead(@Param("conversationId") String conversationId,
                               @Param("username") String username);

    // ─── Image Messages ──────────────────────────────────────────────────────

    /**
     * Retrieve all image messages in group chat (for a media gallery view).
     */
    @Query("SELECT m FROM Message m WHERE m.groupMessage = true " +
           "AND m.messageType = 'IMAGE' ORDER BY m.createdAt DESC")
    List<Message> findGroupImageMessages(Pageable pageable);

    /**
     * Retrieve image messages in a private conversation.
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.messageType = 'IMAGE' ORDER BY m.createdAt DESC")
    List<Message> findPrivateImageMessages(@Param("conversationId") String conversationId);

    // ─── General Queries ─────────────────────────────────────────────────────

    /**
     * Find all messages from a specific sender.
     */
    Page<Message> findBySenderUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    /**
     * Delete all messages older than a given date (for data retention).
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.createdAt < :before")
    int deleteMessagesOlderThan(@Param("before") LocalDateTime before);

    /**
     * Get the last N group messages for initial load.
     */
    @Query(value = "SELECT * FROM messages WHERE is_group_message = 1 " +
                   "ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Message> findLastGroupMessages(@Param("limit") int limit);
}
