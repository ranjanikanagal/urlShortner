package com.assessment.urlshortener.service;

import com.assessment.urlshortener.dto.ShortenRequest;
import com.assessment.urlshortener.dto.ShortenResponse;
import com.assessment.urlshortener.dto.UpdateUrlRequest;

public interface UrlShortenerService {

    ShortenResponse shorten(ShortenRequest request);

    String resolve(String shortCode);

    ShortenResponse update(String shortCode, UpdateUrlRequest request);

    void delete(String shortCode);
}
