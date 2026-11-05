-- =============================================================================
-- V1__init_schema.sql
-- Complete PostgreSQL Production Schema for NeoTactile Messaging, Statuses,
-- Communities, Groups, User Contacts, and WebRTC Audio/Video Call Signaling
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    uid VARCHAR(128) PRIMARY KEY,
    email VARCHAR(200) UNIQUE NOT NULL,
    username VARCHAR(64) UNIQUE NOT NULL,
    phone_number VARCHAR(32) UNIQUE,
    password_hash VARCHAR(255),
    auth_provider VARCHAR(32) NOT NULL DEFAULT 'GOOGLE',
    display_name VARCHAR(80) NOT NULL,
    photo_url VARCHAR(500),
    cover_url VARCHAR(500),
    bio VARCHAR(300),
    short_code VARCHAR(16) UNIQUE NOT NULL,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    is_online BOOLEAN NOT NULL DEFAULT TRUE,
    two_step_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    fcm_token VARCHAR(300),
    last_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_contacts (
    owner_uid VARCHAR(128) NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
    contact_uid VARCHAR(128) NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'ACCEPTED', -- PENDING | ACCEPTED | BLOCKED
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (owner_uid, contact_uid)
);

CREATE TABLE IF NOT EXISTS communities (
    community_id VARCHAR(128) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(400),
    creator_uid VARCHAR(128) NOT NULL REFERENCES users(uid),
    announcement_text VARCHAR(1000),
    avatar_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS conversations (
    chat_id VARCHAR(128) PRIMARY KEY,
    is_group BOOLEAN NOT NULL DEFAULT FALSE,
    group_name VARCHAR(100),
    group_description VARCHAR(400),
    group_avatar_url VARCHAR(500),
    community_id VARCHAR(128) REFERENCES communities(community_id) ON DELETE SET NULL,
    created_by VARCHAR(128) REFERENCES users(uid),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS conversation_members (
    chat_id VARCHAR(128) NOT NULL REFERENCES conversations(chat_id) ON DELETE CASCADE,
    user_uid VARCHAR(128) NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
    role VARCHAR(24) NOT NULL DEFAULT 'MEMBER', -- ADMIN | MEMBER
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (chat_id, user_uid)
);

CREATE TABLE IF NOT EXISTS messages (
    message_id VARCHAR(128) PRIMARY KEY,
    chat_id VARCHAR(128) NOT NULL REFERENCES conversations(chat_id) ON DELETE CASCADE,
    sender_id VARCHAR(128) NOT NULL REFERENCES users(uid),
    receiver_id VARCHAR(128) NOT NULL,
    sender_name VARCHAR(80) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    media_type VARCHAR(24) NOT NULL DEFAULT 'TEXT', -- TEXT | IMAGE | VIDEO | AUDIO | FILE
    media_url VARCHAR(500),
    file_name VARCHAR(200),
    audio_duration_ms BIGINT NOT NULL DEFAULT 0,
    read_status VARCHAR(24) NOT NULL DEFAULT 'SENT', -- SENT | DELIVERED | READ
    reply_to_message_id VARCHAR(128),
    reply_to_sender_name VARCHAR(80),
    reply_to_preview VARCHAR(200),
    is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    reactions VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS status_updates (
    story_id VARCHAR(128) PRIMARY KEY,
    author_id VARCHAR(128) NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
    author_name VARCHAR(80) NOT NULL,
    author_avatar_url VARCHAR(500),
    media_url VARCHAR(500) NOT NULL,
    media_type VARCHAR(24) NOT NULL DEFAULT 'TEXT_CARD', -- TEXT_CARD | IMAGE | VIDEO
    caption VARCHAR(300) NOT NULL,
    privacy_mode VARCHAR(24) NOT NULL DEFAULT 'CONTACTS',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS call_sessions (
    call_id VARCHAR(128) PRIMARY KEY,
    caller_uid VARCHAR(128) NOT NULL REFERENCES users(uid),
    callee_uid VARCHAR(128) NOT NULL REFERENCES users(uid),
    call_type VARCHAR(16) NOT NULL, -- AUDIO | VIDEO
    status VARCHAR(24) NOT NULL, -- RINGING | CONNECTED | REJECTED | ENDED
    sdp_offer TEXT,
    sdp_answer TEXT,
    duration_seconds INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_phone ON users(phone_number);
CREATE INDEX idx_messages_chat_time ON messages(chat_id, created_at);
CREATE INDEX idx_status_expires ON status_updates(expires_at);
