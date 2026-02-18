package com.chatapp.repository;

import com.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity.
 *
 * Provides:
 * - Standard CRUD operations (via JpaRepository)
 * - Custom finders for WebSocket session tracking
 * - Online presence management queries
 *
 * @author ChatApp Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username (case-insensitive).
     * Used for message routing and user lookup.
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Check if a username already exists.
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Find user by WebSocket session ID.
     * Used to identify users on disconnect events.
     */
    Optional<User> findBySessionId(String sessionId);

    /**
     * Find all currently online users.
     * Used to populate the online users sidebar.
     */
    List<User> findByOnlineTrueOrderByUsernameAsc();

    /**
     * Count currently online users.
     */
    long countByOnlineTrue();

    /**
     * Update user's online status and session ID on WebSocket connect.
     */
    @Modifying
    @Query("UPDATE User u SET u.online = :online, u.sessionId = :sessionId, " +
           "u.lastSeen = :lastSeen WHERE u.username = :username")
    int updateOnlineStatus(@Param("username") String username,
                           @Param("online") boolean online,
                           @Param("sessionId") String sessionId,
                           @Param("lastSeen") LocalDateTime lastSeen);

    /**
     * Mark all users as offline (used on application restart).
     */
    @Modifying
    @Query("UPDATE User u SET u.online = false, u.sessionId = null")
    void markAllUsersOffline();
}
