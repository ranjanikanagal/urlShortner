package com.assessment.urlshortener.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One row per redirect. Written asynchronously (see AnalyticsService) so the
 * redirect response never waits on this insert. This is the raw event log
 * that unique-visitor and per-country counts in AnalyticsResponse are
 * aggregated from; UrlMapping.clickCount is a denormalized running total
 * kept in sync from the same async path for a fast "clicks" read.
 */
@Entity
@Table(name = "click_event", indexes = {
        @Index(name = "idx_click_event_short_code", columnList = "shortCode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String shortCode;

    /** SHA-256 hash of IP + User-Agent — identifies a repeat visitor without storing raw PII. */
    @Column(nullable = false, length = 64)
    private String visitorHash;

    @Column(length = 64)
    private String country;

    @Column(nullable = false)
    private Instant clickedAt;
}
