CREATE TABLE artist_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stage_name VARCHAR(100) NOT NULL,
    biography TEXT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_artist_profiles PRIMARY KEY (id),
    CONSTRAINT uq_artist_profiles_user UNIQUE (user_id)
);

CREATE TABLE tracks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    artist_user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    media_object_id BIGINT NOT NULL,
    cover_media_object_id BIGINT NULL,
    lyrics_media_object_id BIGINT NULL,
    duration_ms BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    play_count BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_tracks PRIMARY KEY (id)
);

CREATE TABLE tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT uq_tags_name UNIQUE (name)
);

CREATE TABLE track_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    track_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_track_tags PRIMARY KEY (id),
    CONSTRAINT uq_track_tags_relationship UNIQUE (track_id, tag_id),
    CONSTRAINT fk_track_tags_track FOREIGN KEY (track_id) REFERENCES tracks (id),
    CONSTRAINT fk_track_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE TABLE outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    delivery_status VARCHAR(24) NOT NULL,
    delivery_attempts INT NOT NULL DEFAULT 0,
    available_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT uq_outbox_events_event UNIQUE (event_id)
);

CREATE INDEX idx_tracks_artist_status ON tracks (artist_user_id, status);
CREATE INDEX idx_tracks_status_play_count ON tracks (status, play_count);
CREATE INDEX idx_outbox_delivery ON outbox_events (delivery_status, available_at);
