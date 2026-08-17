package com.autohub.configuration.ratelimit;

import com.autohub.dto.ResponseDto;
import com.autohub.enums.RateLimitType;
import com.autohub.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Spring MVC interceptor that enforces {@link RateLimit} annotations
 * on controller methods.
 * <p>
 * When a rate limit is exceeded, the interceptor short-circuits the request
 * and returns an HTTP 429 (Too Many Requests) response with standard
 * rate-limit headers and a JSON body matching {@link ResponseDto}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true; // Not a controller method — pass through
        }

        // Check method-level annotation first, then class-level
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }
        if (rateLimit == null) {
            return true; // No rate limit annotation — pass through
        }

        String key = resolveKey(rateLimit.type(), request);

        RateLimiterService.ConsumptionResult result = rateLimiterService.tryConsume(
                key,
                rateLimit.capacity(),
                rateLimit.refillTokens(),
                rateLimit.refillDurationInSeconds()
        );

        // Set standard rate-limit headers regardless of outcome
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));

        if (result.allowed()) {
            return true;
        }

        // Rate limit exceeded — throw exception to be handled by GlobalExceptionHandler
        long retryAfterSeconds = Math.max(1, result.retryAfterNanos() / 1_000_000_000);
        throw new com.autohub.exception.RateLimitExceededException(
                "Too many requests. Please try again after " + retryAfterSeconds + " seconds.",
                retryAfterSeconds,
                rateLimit.capacity()
        );
    }

    /**
     * Resolves the bucket key based on the configured {@link RateLimitType}.
     */
    private String resolveKey(RateLimitType type, HttpServletRequest request) {
        String clientIp = ClientIpUtil.getClientIp(request);

        return switch (type) {
            case IP -> "IP:" + clientIp;
            case IP_AND_ENDPOINT -> "IP_EP:" + clientIp + ":" + request.getRequestURI();
            case USER_ID -> {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String principal = (auth != null && auth.isAuthenticated())
                        ? auth.getName()
                        : clientIp; // Fallback to IP if not authenticated
                yield "USER:" + principal;
            }
        };
    }
}
