package com.assessment.urlshortener.scheduler;

import com.assessment.urlshortener.cache.RedisCacheClient;
import com.assessment.urlshortener.model.UrlMapping;
import com.assessment.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Periodically deletes expired mappings. An expired short code is already
 * refused at resolve-time (see UrlShortenerServiceImpl#resolve), so this job
 * is about reclaiming storage and keeping the table lean, not correctness —
 * nothing breaks if it runs a bit late.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredUrlCleanupJob {

    private final UrlMappingRepository repository;
    private final RedisCacheClient cacheClient;

    @Scheduled(fixedDelayString = "${app.cleanup.fixed-delay-ms:3600000}")
    @Transactional
    public void purgeExpiredUrls() {
        List<UrlMapping> expired = repository.findAllByExpiresAtBefore(Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        for (UrlMapping mapping : expired) {
            cacheClient.evict(mapping.getShortCode());
        }
        repository.deleteAll(expired);
        log.info("Cleanup job purged {} expired URL mapping(s)", expired.size());
    }
}
