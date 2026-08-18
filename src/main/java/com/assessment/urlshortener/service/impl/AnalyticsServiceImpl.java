package com.assessment.urlshortener.service.impl;

import com.assessment.urlshortener.dto.AnalyticsResponse;
import com.assessment.urlshortener.exception.UrlNotFoundException;
import com.assessment.urlshortener.model.ClickEvent;
import com.assessment.urlshortener.repository.ClickEventRepository;
import com.assessment.urlshortener.repository.UrlMappingRepository;
import com.assessment.urlshortener.repository.CountryCount;
import com.assessment.urlshortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UrlMappingRepository urlMappingRepository;

    @Override
    @Async("analyticsExecutor")
    @Transactional
    public void recordClickAsync(String shortCode, String visitorHash, String country) {
        try {
            clickEventRepository.save(ClickEvent.builder()
                    .shortCode(shortCode)
                    .visitorHash(visitorHash)
                    .country(country)
                    .clickedAt(Instant.now())
                    .build());
            urlMappingRepository.incrementClickCount(shortCode);
        } catch (Exception e) {
            // Analytics is best-effort: a failure here must never affect the
            // redirect the visitor already received, so it's logged and
            // swallowed rather than propagated (there's no caller left to
            // propagate to on this thread anyway).
            log.warn("Failed to record click for shortCode={}", shortCode, e);
        }
    }

    @Override
    public AnalyticsResponse getAnalytics(String shortCode) {
        var mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        long uniqueVisitors = clickEventRepository.countDistinctVisitors(shortCode);
        Map<String, Long> countries = new LinkedHashMap<>();
        for (CountryCount cc : clickEventRepository.countByCountry(shortCode)) {
            countries.put(cc.country(), cc.count());
        }

        return AnalyticsResponse.builder()
                .clicks(mapping.getClickCount())
                .uniqueVisitors(uniqueVisitors)
                .countries(countries)
                .build();
    }
}
