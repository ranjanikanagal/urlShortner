package com.assessment.urlshortener.controller;

import com.assessment.urlshortener.exception.UrlNotFoundException;
import com.assessment.urlshortener.service.AnalyticsService;
import com.assessment.urlshortener.service.GeoLookupService;
import com.assessment.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlShortenerService urlShortenerService;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private GeoLookupService geoLookupService;

    @Test
    void redirect_knownCode_returns302WithLocationAndRecordsClickAsync() throws Exception {
        when(urlShortenerService.resolve("abc1234")).thenReturn("https://example.com");
        when(geoLookupService.resolveCountry(any())).thenReturn("US");

        mockMvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));

        verify(analyticsService).recordClickAsync(anyString(), anyString(), eq("US"));
    }

    @Test
    void redirect_unknownCode_returns404AndDoesNotRecordClick() throws Exception {
        when(urlShortenerService.resolve("missing")).thenThrow(new UrlNotFoundException("missing"));

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound());

        verify(analyticsService, never()).recordClickAsync(anyString(), anyString(), anyString());
    }
}
