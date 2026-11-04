package com.bootcamp.customer_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Services take "now" from this Clock instead of calling OffsetDateTime.now() directly,
// so unit tests can pass a fixed clock and assert exact timestamps.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
