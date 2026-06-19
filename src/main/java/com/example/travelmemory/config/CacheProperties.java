package com.example.travelmemory.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "travel.cache")
public record CacheProperties(
        Duration latestPriceTtl
) {
    public CacheProperties {
        if (latestPriceTtl == null) {
            latestPriceTtl = Duration.ofMinutes(15);
        }
    }
}
