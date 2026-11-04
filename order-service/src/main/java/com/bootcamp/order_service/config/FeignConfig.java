package com.bootcamp.order_service.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

public class FeignConfig {
    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new ProblemDetailErrorDecoder(objectMapper);
    }
}
