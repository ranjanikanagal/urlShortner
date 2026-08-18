-- Duplicate-submission detection: hash of the normalized original URL.
-- Nullable + a plain (non-unique) index rather than a hard unique constraint,
-- because custom-alias entries deliberately allow re-shortening a URL that
-- already has a generated short code (see UrlShortenerServiceImpl#shorten).
ALTER TABLE url_mapping ADD COLUMN IF NOT EXISTS original_url_hash VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_original_url_hash ON url_mapping (original_url_hash);

-- Raw click events, aggregated on read by AnalyticsService for unique
-- visitors and per-country counts. Written asynchronously from the redirect
-- path — see AnalyticsServiceImpl#recordClickAsync.
CREATE TABLE IF NOT EXISTS click_event (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL,
    visitor_hash VARCHAR(64) NOT NULL,
    country VARCHAR(64),
    clicked_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_click_event_short_code ON click_event (short_code);
