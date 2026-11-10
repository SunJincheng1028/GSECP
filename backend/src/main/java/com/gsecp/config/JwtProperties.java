package com.gsecp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "gsecp.jwt")
public record JwtProperties(
        @DefaultValue("gsecp-dev-only-jwt-secret-key-change-me-2026") String secret,
        @DefaultValue("86400000") Long expirationMs
) {
}
