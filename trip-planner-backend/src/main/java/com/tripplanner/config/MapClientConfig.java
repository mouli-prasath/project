package com.tripplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MapClientConfig {

    /**
     * Shared RestClient for outbound calls to Google's Geocoding and Places
     * APIs. Kept as a single bean so connection pooling/timeouts can be
     * tuned in one place later if needed.
     */
    @Bean
    public RestClient mapsRestClient() {
        return RestClient.builder().build();
    }
}
