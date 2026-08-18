package com.assessment.urlshortener.controller;

import com.assessment.urlshortener.dto.ShortenRequest;
import com.assessment.urlshortener.dto.ShortenResponse;
import com.assessment.urlshortener.dto.UpdateUrlRequest;
import com.assessment.urlshortener.exception.UrlNotFoundException;
import com.assessment.urlshortener.service.UrlShortenerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlShortenerService urlShortenerService;

    @Test
    void shorten_returnsCreatedWithShortUrl() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com/some/path");

        ShortenResponse response = ShortenResponse.builder()
                .shortCode("Ax7KfP")
                .shortUrl("http://localhost:8080/Ax7KfP")
                .originalUrl(request.getUrl())
                .createdAt(Instant.now())
                .build();

        when(urlShortenerService.shorten(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/Ax7KfP"));
    }

    @Test
    void shorten_invalidUrl_returnsBadRequest() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("not-a-url");

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsUpdatedMapping() throws Exception {
        UpdateUrlRequest request = new UpdateUrlRequest();
        request.setUrl("https://example.com/new");

        ShortenResponse response = ShortenResponse.builder()
                .shortCode("Ax7KfP")
                .shortUrl("http://localhost:8080/Ax7KfP")
                .originalUrl("https://example.com/new")
                .createdAt(Instant.now())
                .build();
        when(urlShortenerService.update(eq("Ax7KfP"), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/shorten/Ax7KfP")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/new"));
    }

    @Test
    void update_unknownCode_returnsNotFound() throws Exception {
        when(urlShortenerService.update(eq("missing"), any()))
                .thenThrow(new UrlNotFoundException("missing"));

        mockMvc.perform(put("/api/v1/shorten/missing")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/shorten/Ax7KfP"))
                .andExpect(status().isNoContent());
    }
}
