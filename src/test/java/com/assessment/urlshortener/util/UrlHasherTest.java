package com.assessment.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlHasherTest {

    @Test
    void hash_isDeterministic() {
        assertThat(UrlHasher.hash("https://example.com")).isEqualTo(UrlHasher.hash("https://example.com"));
    }

    @Test
    void hash_isCaseAndWhitespaceInsensitive() {
        assertThat(UrlHasher.hash("https://Example.com "))
                .isEqualTo(UrlHasher.hash("https://example.com"));
    }

    @Test
    void hash_differentUrls_produceDifferentHashes() {
        assertThat(UrlHasher.hash("https://example.com/a"))
                .isNotEqualTo(UrlHasher.hash("https://example.com/b"));
    }
}
