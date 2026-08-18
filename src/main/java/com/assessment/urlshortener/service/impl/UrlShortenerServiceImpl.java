package com.assessment.urlshortener.service.impl;

import com.assessment.urlshortener.cache.RedisCacheClient;
import com.assessment.urlshortener.dto.ShortenRequest;
import com.assessment.urlshortener.dto.ShortenResponse;
import com.assessment.urlshortener.dto.UpdateUrlRequest;
import com.assessment.urlshortener.exception.AliasAlreadyExistsException;
import com.assessment.urlshortener.exception.UrlExpiredException;
import com.assessment.urlshortener.exception.UrlNotFoundException;
import com.assessment.urlshortener.model.UrlMapping;
import com.assessment.urlshortener.repository.UrlMappingRepository;
import com.assessment.urlshortener.service.ShortCodeGenerator;
import com.assessment.urlshortener.service.UrlShortenerService;
import com.assessment.urlshortener.util.UrlHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Cache-aside implementation: reads try Redis first (via RedisCacheClient,
 * which handles retry/degradation) and fall back to Postgres on a miss,
 * repopulating the cache. Writes go to Postgres first (source of truth)
 * then populate the cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final UrlMappingRepository repository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final RedisCacheClient cacheClient;

    @Value("${app.cache.ttl-seconds:3600}")
    private long cacheTtlSeconds;

    @Value("${app.url.default-expiry-days:365}")
    private long defaultExpiryDays;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        boolean isCustomAlias = request.getCustomAlias() != null && !request.getCustomAlias().isBlank();
        String urlHash = UrlHasher.hash(request.getUrl());

        // Duplicate detection only short-circuits generated (non-custom)
        // requests: a custom alias is an explicit, distinct ask from the
        // caller and should always create its own row even if the same
        // destination URL was shortened before.
        if (!isCustomAlias) {
            var existing = repository.findByOriginalUrlHash(urlHash);
            if (existing.isPresent() && !isExpired(existing.get())) {
                log.info("Duplicate URL submitted, returning existing shortCode={}", existing.get().getShortCode());
                return toResponse(existing.get());
            }
        }

        String shortCode = isCustomAlias
                ? claimCustomAlias(request.getCustomAlias())
                : generateUniqueShortCode();

        Instant expiresAt = resolveExpiry(request.getExpiresAt());

        UrlMapping mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getUrl())
                .originalUrlHash(isCustomAlias ? null : urlHash)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .clickCount(0L)
                .build();

        repository.save(mapping);
        cachePut(shortCode, mapping.getOriginalUrl(), expiresAt);
        log.info("Created shortCode={} expiresAt={}", shortCode, expiresAt);

        return toResponse(mapping);
    }

    @Override
    public String resolve(String shortCode) {
        String cached = cacheClient.get(shortCode);
        if (cached != null) {
            log.debug("Cache hit for shortCode={}", shortCode);
            return cached;
        }

        log.debug("Cache miss for shortCode={}, falling back to DB", shortCode);
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (isExpired(mapping)) {
            throw new UrlExpiredException(shortCode);
        }

        cachePut(shortCode, mapping.getOriginalUrl(), mapping.getExpiresAt());
        return mapping.getOriginalUrl();
    }

    @Override
    @Transactional
    public ShortenResponse update(String shortCode, UpdateUrlRequest request) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            mapping.setOriginalUrl(request.getUrl());
            mapping.setOriginalUrlHash(UrlHasher.hash(request.getUrl()));
        }
        if (request.getExpiresAt() != null) {
            mapping.setExpiresAt(request.getExpiresAt().atStartOfDay(ZoneOffset.UTC).toInstant());
        }

        repository.save(mapping);
        cachePut(shortCode, mapping.getOriginalUrl(), mapping.getExpiresAt());
        log.info("Updated shortCode={}", shortCode);

        return toResponse(mapping);
    }

    @Override
    @Transactional
    public void delete(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        repository.delete(mapping);
        cacheClient.evict(shortCode);
        log.info("Deleted shortCode={}", shortCode);
    }

    private boolean isExpired(UrlMapping mapping) {
        return mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(Instant.now());
    }

    private Instant resolveExpiry(LocalDate requested) {
        LocalDate effective = requested != null ? requested : LocalDate.now().plusDays(defaultExpiryDays);
        return effective.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private String claimCustomAlias(String alias) {
        if (repository.existsByShortCode(alias)) {
            throw new AliasAlreadyExistsException(alias);
        }
        return alias;
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = shortCodeGenerator.generate();
            if (!repository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique short code after "
                + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    private void cachePut(String shortCode, String originalUrl, Instant expiresAt) {
        Duration ttl = expiresAt != null
                ? Duration.between(Instant.now(), expiresAt)
                : Duration.ofSeconds(cacheTtlSeconds);
        cacheClient.put(shortCode, originalUrl, ttl);
    }

    private ShortenResponse toResponse(UrlMapping mapping) {
        LocalDate expiresAt = mapping.getExpiresAt() != null
                ? mapping.getExpiresAt().atZone(ZoneOffset.UTC).toLocalDate()
                : null;

        return ShortenResponse.builder()
                .shortCode(mapping.getShortCode())
                .shortUrl(baseUrl + "/" + mapping.getShortCode())
                .originalUrl(mapping.getOriginalUrl())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(expiresAt)
                .build();
    }
}
