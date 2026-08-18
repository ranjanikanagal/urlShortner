package com.assessment.urlshortener.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUrlRequest {

    /** Optional — omit to leave the destination URL unchanged and only update expiry. */
    @Pattern(regexp = "^(https?)://.+", message = "url must be a valid http(s) URL")
    private String url;

    @FutureOrPresent(message = "expiresAt must not be in the past")
    private LocalDate expiresAt;
}
