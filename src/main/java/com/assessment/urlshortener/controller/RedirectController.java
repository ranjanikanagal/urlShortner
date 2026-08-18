package com.assessment.urlshortener.controller;

import com.assessment.urlshortener.service.AnalyticsService;
import com.assessment.urlshortener.service.GeoLookupService;
import com.assessment.urlshortener.service.UrlShortenerService;
import com.assessment.urlshortener.util.VisitorHasher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Redirect", description = "Resolve a short code to its original URL")
public class RedirectController {

    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;
    private final GeoLookupService geoLookupService;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to the original URL")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String originalUrl = urlShortenerService.resolve(shortCode);

        // Fire-and-forget: the visitor gets the 302 immediately, analytics
        // writes happen on a separate pool and never delay this response.
        String visitorHash = VisitorHasher.hash(request.getRemoteAddr(), request.getHeader("User-Agent"));
        String country = geoLookupService.resolveCountry(request);
        analyticsService.recordClickAsync(shortCode, visitorHash, country);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
