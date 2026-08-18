package com.assessment.urlshortener.controller;

import com.assessment.urlshortener.dto.AnalyticsResponse;
import com.assessment.urlshortener.exception.UrlNotFoundException;
import com.assessment.urlshortener.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void analytics_returnsClicksUniqueVisitorsAndCountries() throws Exception {
        AnalyticsResponse response = AnalyticsResponse.builder()
                .clicks(1045)
                .uniqueVisitors(810)
                .countries(Map.of("US", 420L, "India", 300L))
                .build();
        when(analyticsService.getAnalytics("Ax7KfP")).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/Ax7KfP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").value(1045))
                .andExpect(jsonPath("$.uniqueVisitors").value(810))
                .andExpect(jsonPath("$.countries.US").value(420));
    }

    @Test
    void analytics_unknownCode_returnsNotFound() throws Exception {
        when(analyticsService.getAnalytics("missing")).thenThrow(new UrlNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/analytics/missing"))
                .andExpect(status().isNotFound());
    }
}
