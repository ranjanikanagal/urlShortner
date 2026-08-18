package com.assessment.urlshortener.service;

import com.assessment.urlshortener.util.Base62Encoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_LENGTH = 7;

    /**
     * Generates a random Base62 short code. Collision handling (retry-on-conflict)
     * is the caller's responsibility since uniqueness depends on what's already
     * persisted in the database.
     */
    public String generate() {
        long randomValue = Math.abs(RANDOM.nextLong());
        String encoded = Base62Encoder.encode(randomValue);
        return encoded.length() > DEFAULT_LENGTH
                ? encoded.substring(0, DEFAULT_LENGTH)
                : encoded;
    }
}
