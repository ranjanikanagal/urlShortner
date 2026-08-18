package com.assessment.urlshortener.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes IP + User-Agent into an opaque visitor identifier for unique-visitor
 * counting, so no raw IP address is ever persisted.
 */
public final class VisitorHasher {

    private VisitorHasher() {
    }

    public static String hash(String ipAddress, String userAgent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = (ipAddress == null ? "" : ipAddress) + "|" + (userAgent == null ? "" : userAgent);
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
