package com.autohub.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Explicit ObjectMapper bean for JSON (de)serialization across the app,
     * including WhatsAppClient's request/response payload handling.
     * Registering JavaTimeModule ensures LocalDateTime/LocalDate fields
     * (e.g. CustomerLead.enquiryDate) serialize correctly instead of failing
     * or dumping raw epoch arrays.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}