package com.assessment.urlshortener.service.impl;

import com.assessment.urlshortener.service.GeoLookupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * No bundled GeoIP database or paid lookup service is wired into this build
 * (see docs/LIMITATIONS.md). Instead this reads a country hint from a header
 * a real edge/CDN layer (Cloudflare's CF-IPCountry, or an API gateway) would
 * typically inject, and falls back to "Unknown". This keeps the analytics
 * pipeline (hashing, storage, aggregation) fully real and swappable — only
 * this one resolver needs replacing with a real MaxMind GeoLite2 lookup to
 * go to production.
 */
@Service
public class StubGeoLookupService implements GeoLookupService {

    private static final String COUNTRY_HEADER = "X-Country";
    private static final String CDN_COUNTRY_HEADER = "CF-IPCountry";
    private static final String UNKNOWN = "Unknown";

    @Override
    public String resolveCountry(HttpServletRequest request) {
        String header = request.getHeader(COUNTRY_HEADER);
        if (header == null || header.isBlank()) {
            header = request.getHeader(CDN_COUNTRY_HEADER);
        }
        return (header == null || header.isBlank()) ? UNKNOWN : header;
    }
}
