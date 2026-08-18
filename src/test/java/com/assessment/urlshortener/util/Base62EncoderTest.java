package com.assessment.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Base62EncoderTest {

    @Test
    void encode_zero_returnsFirstCharacter() {
        assertThat(Base62Encoder.encode(0)).isEqualTo("0");
    }

    @Test
    void encode_isDeterministic() {
        long value = 123456789L;
        assertThat(Base62Encoder.encode(value)).isEqualTo(Base62Encoder.encode(value));
    }

    @Test
    void encode_differentValues_produceDifferentCodes() {
        assertThat(Base62Encoder.encode(1)).isNotEqualTo(Base62Encoder.encode(2));
    }
}
