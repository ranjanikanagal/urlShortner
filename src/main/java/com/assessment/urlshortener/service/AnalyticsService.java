package com.assessment.urlshortener.service;

import com.assessment.urlshortener.dto.AnalyticsResponse;

public interface AnalyticsService {

    /**
     * Records a single click. Called fire-and-forget from the redirect path
     * (see RedirectController) so analytics writes never add latency to the
     * 302 response — the redirect returns before this necessarily completes.
     * This is the "asynchronous analytics, eventual consistency" trade-off
     * documented in docs/TRADEOFFS.md.
     */
    void recordClickAsync(String shortCode, String visitorHash, String country);

    AnalyticsResponse getAnalytics(String shortCode);
}
