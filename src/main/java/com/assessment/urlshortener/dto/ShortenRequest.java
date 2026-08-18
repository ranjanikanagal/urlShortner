package com.assessment.urlshortener.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ShortenRequest {

    @NotBlank(message = "url must not be blank")
    @Pattern(regexp = "^(https?)://.+", message = "url must be a valid http(s) URL")
    private String url;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{4,16}$", message = "customAlias must be 4-16 alphanumeric characters")
    private String customAlias;

    /** Optional. Defaults to app.url.default-expiry-days from now if omitted. */
    @FutureOrPresent(message = "expiresAt must not be in the past")
    private LocalDate expiresAt;
}
