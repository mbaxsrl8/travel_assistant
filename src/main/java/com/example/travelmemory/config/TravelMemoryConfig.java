package com.example.travelmemory.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        CacheProperties.class,
        HBaseProperties.class
})
public class TravelMemoryConfig {
}
