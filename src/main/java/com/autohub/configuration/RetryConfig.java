package com.autohub.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate whatsAppRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(HttpServerErrorException.class, true);   // 5xx from Meta
        retryableExceptions.put(ResourceAccessException.class, true);    // timeouts / connection issues

        RetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);   // 1s
        backOffPolicy.setMultiplier(2.0);           // 1s -> 2s -> 4s
        backOffPolicy.setMaxInterval(10_000L);      // cap at 10s

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}