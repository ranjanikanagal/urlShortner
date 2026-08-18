package com.assessment.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class ShortenResponse {
    // shortUrl + expiresAt match the spec's response shape exactly.
    // shortCode / originalUrl / createdAt are additive: real clients need the
    // bare code (e.g. to build their own links) and a createdAt audit field,
    // and echoing originalUrl back lets a client verify what was actually
    // stored without a second call. Nothing in the spec's shape was removed.
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Instant createdAt;
    private LocalDate expiresAt;
}
