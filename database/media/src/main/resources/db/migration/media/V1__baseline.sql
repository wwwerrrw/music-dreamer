CREATE TABLE media_objects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    media_type VARCHAR(24) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    byte_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_media_objects PRIMARY KEY (id),
    CONSTRAINT uq_media_objects_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_media_objects_owner_type ON media_objects (owner_user_id, media_type);
CREATE INDEX idx_media_objects_sha256 ON media_objects (sha256);
