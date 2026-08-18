package com.assessment.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class AnalyticsResponse {
    private long clicks;
    private long uniqueVisitors;
    private Map<String, Long> countries;
}
