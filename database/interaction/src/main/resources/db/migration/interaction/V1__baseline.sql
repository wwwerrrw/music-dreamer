CREATE TABLE track_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    track_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_track_favorites PRIMARY KEY (id),
    CONSTRAINT uq_track_favorites_relationship UNIQUE (user_id, track_id)
);

CREATE TABLE playlists (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    visibility VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_playlists PRIMARY KEY (id)
);

CREATE TABLE playlist_tracks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    playlist_id BIGINT NOT NULL,
    track_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_playlist_tracks PRIMARY KEY (id),
    CONSTRAINT uq_playlist_tracks_relationship UNIQUE (playlist_id, track_id),
    CONSTRAINT fk_playlist_tracks_playlist FOREIGN KEY (playlist_id) REFERENCES playlists (id)
);

CREATE TABLE playlist_follows (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    playlist_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_playlist_follows PRIMARY KEY (id),
    CONSTRAINT uq_playlist_follows_relationship UNIQUE (user_id, playlist_id),
    CONSTRAINT fk_playlist_follows_playlist FOREIGN KEY (playlist_id) REFERENCES playlists (id)
);

CREATE TABLE listening_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    track_id BIGINT NOT NULL,
    position_ms BIGINT NOT NULL DEFAULT 0,
    last_listened_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_listening_history PRIMARY KEY (id),
    CONSTRAINT uq_listening_history_position UNIQUE (user_id, track_id)
);

CREATE TABLE messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_user_id BIGINT NULL,
    audience_type VARCHAR(24) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    published_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_messages PRIMARY KEY (id)
);

CREATE TABLE message_receipts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_message_receipts PRIMARY KEY (id),
    CONSTRAINT uq_message_receipts_recipient UNIQUE (message_id, user_id),
    CONSTRAINT fk_message_receipts_message FOREIGN KEY (message_id) REFERENCES messages (id)
);

CREATE TABLE audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    operator_user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_id VARCHAR(64) NOT NULL,
    result VARCHAR(24) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_audit_events PRIMARY KEY (id),
    CONSTRAINT uq_audit_events_event UNIQUE (event_id)
);

CREATE INDEX idx_track_favorites_user ON track_favorites (user_id, created_at);
CREATE INDEX idx_playlists_owner ON playlists (owner_user_id, updated_at);
CREATE INDEX idx_listening_history_recent ON listening_history (user_id, last_listened_at);
CREATE INDEX idx_message_receipts_unread ON message_receipts (user_id, read_at);
CREATE INDEX idx_audit_events_trace ON audit_events (trace_id);
