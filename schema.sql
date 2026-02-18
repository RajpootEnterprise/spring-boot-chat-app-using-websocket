-- ============================================================
-- RealTime Chat Application - MySQL Schema
-- Version: 1.0.0
-- Description: Creates all tables required for the chat app.
--              Run this script ONCE before starting the application.
-- ============================================================

-- Create and select the database
CREATE DATABASE IF NOT EXISTS chatdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chatdb;

-- ============================================================
-- Create dedicated MySQL user (run as root)
-- ============================================================
CREATE USER IF NOT EXISTS 'chatuser'@'localhost' IDENTIFIED BY 'chatpassword';
GRANT ALL PRIVILEGES ON chatdb.* TO 'chatuser'@'localhost';
FLUSH PRIVILEGES;

-- ============================================================
-- Table: users
-- Stores registered chat participants.
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    display_name    VARCHAR(100),
    session_id      VARCHAR(100),               -- Current WebSocket session ID
    is_online       TINYINT(1)      NOT NULL DEFAULT 0,
    avatar_url      VARCHAR(255),
    last_seen       DATETIME,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY  uq_users_username   (username),
    INDEX       idx_users_online    (is_online),
    INDEX       idx_users_session   (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: messages
-- Stores all chat messages (group and private).
-- ============================================================
CREATE TABLE IF NOT EXISTS messages (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,

    -- Message content
    content                 TEXT,                       -- Null for image-only messages
    message_type            ENUM('TEXT','IMAGE','SYSTEM') NOT NULL DEFAULT 'TEXT',

    -- Sender and receiver
    sender_id               BIGINT          NOT NULL,
    receiver_id             BIGINT,                     -- Null for group messages

    -- Conversation tracking (for private chat history)
    conversation_id         VARCHAR(100),               -- e.g. "user_3_user_7"

    -- Group vs private flag
    is_group_message        TINYINT(1)      NOT NULL DEFAULT 0,

    -- Image metadata (populated when message_type = 'IMAGE')
    image_url               VARCHAR(255),               -- /uploads/images/uuid.jpg
    image_original_name     VARCHAR(255),               -- Original uploaded filename
    image_content_type      VARCHAR(50),                -- MIME type: image/jpeg, etc.
    image_size_bytes        BIGINT,                     -- File size for display

    -- Status
    is_read                 TINYINT(1)      NOT NULL DEFAULT 0,

    -- Audit
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    -- Foreign keys
    CONSTRAINT fk_msg_sender    FOREIGN KEY (sender_id)   REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_receiver  FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Performance indexes
    INDEX idx_messages_sender          (sender_id),
    INDEX idx_messages_receiver        (receiver_id),
    INDEX idx_messages_type            (message_type),
    INDEX idx_messages_timestamp       (created_at DESC),
    INDEX idx_messages_conversation    (conversation_id),
    INDEX idx_messages_group           (is_group_message, created_at DESC),

    -- Composite index for unread count queries
    INDEX idx_messages_unread          (receiver_id, is_read, is_group_message)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: image_metadata
-- Optional separate table for rich image tracking.
-- The messages table stores basic image info.
-- This table allows for future gallery/search features.
-- ============================================================
CREATE TABLE IF NOT EXISTS image_metadata (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    message_id      BIGINT          NOT NULL,
    file_path       VARCHAR(500)    NOT NULL,           -- Absolute server path
    url_path        VARCHAR(255)    NOT NULL,           -- Public URL path
    original_name   VARCHAR(255)    NOT NULL,
    content_type    VARCHAR(50)     NOT NULL,
    size_bytes      BIGINT          NOT NULL,
    width_px        INT,                                -- Image dimensions (if extracted)
    height_px       INT,
    uploaded_by     BIGINT          NOT NULL,           -- User ID
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_imgmeta_message  FOREIGN KEY (message_id)  REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_imgmeta_user     FOREIGN KEY (uploaded_by) REFERENCES users(id)    ON DELETE CASCADE,
    INDEX idx_imgmeta_message  (message_id),
    INDEX idx_imgmeta_user     (uploaded_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Sample seed data for testing (optional)
-- ============================================================

-- Insert test users (passwords not needed since we use username-only auth)
INSERT IGNORE INTO users (username, display_name, is_online, created_at) VALUES
    ('alice',   'Alice Johnson', 0, NOW()),
    ('bob',     'Bob Smith',     0, NOW()),
    ('charlie', 'Charlie Brown', 0, NOW());

-- Insert sample group messages for testing
INSERT IGNORE INTO messages (content, message_type, sender_id, is_group_message, created_at)
SELECT 'Welcome to ChatApp! 🎉', 'TEXT', id, 1, NOW()
FROM users WHERE username = 'alice'
LIMIT 1;

INSERT IGNORE INTO messages (content, message_type, sender_id, is_group_message, created_at)
SELECT 'Hey everyone! Glad to be here.', 'TEXT', id, 1, NOW()
FROM users WHERE username = 'bob'
LIMIT 1;

-- ============================================================
-- Useful queries for monitoring (reference only)
-- ============================================================

-- View online users:
-- SELECT username, display_name, last_seen FROM users WHERE is_online = 1;

-- View recent group messages:
-- SELECT m.content, u.username, m.created_at FROM messages m
-- JOIN users u ON m.sender_id = u.id
-- WHERE m.is_group_message = 1 ORDER BY m.created_at DESC LIMIT 20;

-- View private conversation:
-- SELECT m.content, s.username as sender, r.username as receiver, m.created_at
-- FROM messages m
-- JOIN users s ON m.sender_id   = s.id
-- JOIN users r ON m.receiver_id = r.id
-- WHERE m.conversation_id = 'user_1_user_2'
-- ORDER BY m.created_at;

-- Count unread messages per user:
-- SELECT r.username, COUNT(*) as unread FROM messages m
-- JOIN users r ON m.receiver_id = r.id
-- WHERE m.is_read = 0 AND m.is_group_message = 0
-- GROUP BY r.username;
