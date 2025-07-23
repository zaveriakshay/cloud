package com.cloud.docs.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configures web-related beans, including Jackson mappers for JSON and YAML.
 */
@Configuration
public class WebConfig {

    /**
     * Defines the primary, application-wide ObjectMapper for JSON serialization.
     * The @Primary annotation is crucial. It tells Spring to use this bean by default
     * whenever an ObjectMapper is needed, which includes serializing responses
     * from @RestController endpoints.
     *
     * @return A configured ObjectMapper for JSON.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register the JavaTimeModule to properly handle Java 8 date/time types (e.g., Instant)
        mapper.registerModule(new JavaTimeModule());
        // Configure it to not write dates as timestamps, which is more human-readable.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

}