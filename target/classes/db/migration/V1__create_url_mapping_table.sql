CREATE TABLE IF NOT EXISTS url_mapping (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    click_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_short_code UNIQUE (short_code)
);

CREATE INDEX idx_url_mapping_created_at ON url_mapping (created_at);
