package com.assessment.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorHasherTest {

    @Test
    void hash_sameIpAndUserAgent_producesSameHash() {
        String h1 = VisitorHasher.hash("203.0.113.5", "Mozilla/5.0");
        String h2 = VisitorHasher.hash("203.0.113.5", "Mozilla/5.0");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void hash_differentIp_producesDifferentHash() {
        String h1 = VisitorHasher.hash("203.0.113.5", "Mozilla/5.0");
        String h2 = VisitorHasher.hash("203.0.113.6", "Mozilla/5.0");
        assertThat(h1).isNotEqualTo(h2);
    }
}
