package com.assessment.urlshortener.service;

import jakarta.servlet.http.HttpServletRequest;

public interface GeoLookupService {

    /**
     * Resolves a country label for a request. See StubGeoLookupService for
     * why this is a stub in this build rather than a real MaxMind/IP2Location
     * lookup — documented in docs/LIMITATIONS.md.
     */
    String resolveCountry(HttpServletRequest request);
}
