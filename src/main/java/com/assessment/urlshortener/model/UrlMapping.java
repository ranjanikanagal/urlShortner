package com.assessment.urlshortener.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "url_mapping", indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true),
        @Index(name = "idx_original_url_hash", columnList = "originalUrlHash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    /**
     * SHA-256 hex digest of the normalized original URL. Used to detect
     * duplicate submissions without scanning/comparing the full 2048-char
     * URL column. Nullable at the DB level (partial unique index) so a
     * custom-alias entry pointing at an already-shortened URL can still be
     * created deliberately without tripping a hard uniqueness violation.
     */
    @Column(length = 64)
    private String originalUrlHash;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;

    @Builder.Default
    private long clickCount = 0L;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
