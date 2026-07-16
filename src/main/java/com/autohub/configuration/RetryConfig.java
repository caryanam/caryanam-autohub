package com.autohub.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

    /**
     * Facebook publish worker uses WebClient (reactive), which throws
     * WebClientResponseException (HTTP error status) / WebClientRequestException
     * (connection/timeout) rather than RestTemplate's exception types - so
     * this needs its own policy rather than reusing whatsAppRetryTemplate.
     * Retries: network, timeout, 5xx, rate limit (429).
     * Does NOT retry: invalid token, permission missing, invalid image -
     * those are caught and classified as permanent inside FacebookGraphClient
     * before ever reaching this template.
     */
    @Bean
    public RetryTemplate facebookRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(WebClientResponseException.class, true);
        retryableExceptions.put(WebClientRequestException.class, true);

        RetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10_000L);

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}