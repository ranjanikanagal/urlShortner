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
import com.assessment.urlshortener.util.UrlHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceImplTest {

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private RedisCacheClient cacheClient;

    @InjectMocks
    private UrlShortenerServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "cacheTtlSeconds", 3600L);
        ReflectionTestUtils.setField(service, "defaultExpiryDays", 365L);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
    }

    @Test
    void shorten_generatesCodeAndPersistsMapping() {
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        when(repository.existsByShortCode("abc1234")).thenReturn(false);
        when(repository.findByOriginalUrlHash(anyString())).thenReturn(Optional.empty());

        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com/very/long/path");

        ShortenResponse response = service.shorten(request);

        assertThat(response.getShortCode()).isEqualTo("abc1234");
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/abc1234");
        assertThat(response.getOriginalUrl()).isEqualTo(request.getUrl());
        assertThat(response.getExpiresAt()).isEqualTo(LocalDate.now().plusDays(365));
        verify(repository).save(any(UrlMapping.class));
        verify(cacheClient).put(eq("abc1234"), eq(request.getUrl()), any());
    }

    @Test
    void shorten_duplicateUrl_returnsExistingMappingWithoutCreatingNewRow() {
        String urlHash = UrlHasher.hash("https://example.com/dup");
        UrlMapping existing = UrlMapping.builder()
                .shortCode("existing")
                .originalUrl("https://example.com/dup")
                .originalUrlHash(urlHash)
                .createdAt(Instant.now())
                .build();
        when(repository.findByOriginalUrlHash(urlHash)).thenReturn(Optional.of(existing));

        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com/dup");

        ShortenResponse response = service.shorten(request);

        assertThat(response.getShortCode()).isEqualTo("existing");
        verify(repository, never()).save(any());
        verify(shortCodeGenerator, never()).generate();
    }

    @Test
    void shorten_withCustomAlias_skipsDuplicateCheckAndRejectsTakenAlias() {
        when(repository.existsByShortCode("mylink")).thenReturn(true);

        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com");
        request.setCustomAlias("mylink");

        assertThatThrownBy(() -> service.shorten(request))
                .isInstanceOf(AliasAlreadyExistsException.class);

        verify(repository, never()).save(any());
        verify(repository, never()).findByOriginalUrlHash(anyString());
    }

    @Test
    void resolve_cacheHit_skipsDb() {
        when(cacheClient.get("abc1234")).thenReturn("https://example.com");

        String result = service.resolve("abc1234");

        assertThat(result).isEqualTo("https://example.com");
        verify(repository, never()).findByShortCode(anyString());
    }

    @Test
    void resolve_cacheMiss_fallsBackToDatabaseAndRepopulatesCache() {
        when(cacheClient.get("abc1234")).thenReturn(null);

        UrlMapping mapping = UrlMapping.builder()
                .shortCode("abc1234")
                .originalUrl("https://example.com")
                .createdAt(Instant.now())
                .build();
        when(repository.findByShortCode("abc1234")).thenReturn(Optional.of(mapping));

        String result = service.resolve("abc1234");

        assertThat(result).isEqualTo("https://example.com");
        verify(cacheClient).put(eq("abc1234"), eq("https://example.com"), any());
    }

    @Test
    void resolve_unknownCode_throwsNotFound() {
        when(cacheClient.get("missing")).thenReturn(null);
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("missing"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    void resolve_expiredMapping_throwsExpired() {
        when(cacheClient.get("expired")).thenReturn(null);

        UrlMapping mapping = UrlMapping.builder()
                .shortCode("expired")
                .originalUrl("https://example.com")
                .createdAt(Instant.now().minusSeconds(120))
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        when(repository.findByShortCode("expired")).thenReturn(Optional.of(mapping));

        assertThatThrownBy(() -> service.resolve("expired"))
                .isInstanceOf(UrlExpiredException.class);
    }

    @Test
    void update_changesUrlAndExpiry() {
        UrlMapping mapping = UrlMapping.builder()
                .shortCode("abc1234")
                .originalUrl("https://old.example.com")
                .createdAt(Instant.now())
                .build();
        when(repository.findByShortCode("abc1234")).thenReturn(Optional.of(mapping));

        UpdateUrlRequest request = new UpdateUrlRequest();
        request.setUrl("https://new.example.com");
        request.setExpiresAt(LocalDate.now().plusDays(30));

        ShortenResponse response = service.update("abc1234", request);

        assertThat(response.getOriginalUrl()).isEqualTo("https://new.example.com");
        assertThat(response.getExpiresAt()).isEqualTo(LocalDate.now().plusDays(30));
        verify(cacheClient).put(eq("abc1234"), eq("https://new.example.com"), any());
    }

    @Test
    void update_unknownCode_throwsNotFound() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("missing", new UpdateUrlRequest()))
                .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    void delete_knownCode_removesFromDbAndCache() {
        UrlMapping mapping = UrlMapping.builder()
                .shortCode("abc1234")
                .originalUrl("https://example.com")
                .createdAt(Instant.now())
                .build();
        when(repository.findByShortCode("abc1234")).thenReturn(Optional.of(mapping));

        service.delete("abc1234");

        verify(repository).delete(mapping);
        verify(cacheClient).evict("abc1234");
    }
}
