package com.assessment.urlshortener.controller;

import com.assessment.urlshortener.dto.ShortenRequest;
import com.assessment.urlshortener.dto.ShortenResponse;
import com.assessment.urlshortener.dto.UpdateUrlRequest;
import com.assessment.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/shorten")
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Create, update, and delete shortened URLs")
public class UrlController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping
    @Operation(summary = "Create a shortened URL")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        log.info("Shorten request received for url length={}", request.getUrl().length());
        ShortenResponse response = urlShortenerService.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{shortCode}")
    @Operation(summary = "Update the destination URL and/or expiry of a short code")
    public ResponseEntity<ShortenResponse> update(@PathVariable String shortCode,
                                                   @Valid @RequestBody UpdateUrlRequest request) {
        return ResponseEntity.ok(urlShortenerService.update(shortCode, request));
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Delete a short URL")
    public ResponseEntity<Void> delete(@PathVariable String shortCode) {
        urlShortenerService.delete(shortCode);
        return ResponseEntity.noContent().build();
    }
}
