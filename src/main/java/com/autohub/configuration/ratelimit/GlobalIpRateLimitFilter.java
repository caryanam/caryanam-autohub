package com.autohub.configuration.ratelimit;

import com.autohub.util.ClientIpUtil;
import com.autohub.exception.RateLimitExceededException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Early-stage servlet filter providing global IP-based flood protection.
 * <p>
 * This filter runs before Spring Security and Spring MVC, acting as the
 * first line of DDoS / flood defense. It enforces a configurable
 * per-IP request quota (default: 300 requests / minute).
 * </p>
 * <p>
 * The following paths are <strong>whitelisted</strong> and bypass this filter entirely:
 * <ul>
 *   <li>{@code /api/auth/login} — Login API is exempt from rate limiting</li>
 *   <li>{@code /api/webhook/**} — Meta/WhatsApp webhooks must never be dropped</li>
 *   <li>{@code /uploads/**} — Static file serving</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GlobalIpRateLimitFilter implements Filter {

    private final RateLimiterService rateLimiterService;
    private final HandlerExceptionResolver resolver;

    @Value("${ratelimit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${ratelimit.global.ip.capacity:300}")
    private long globalCapacity;

    @Value("${ratelimit.global.ip.refill-duration-seconds:60}")
    private long globalRefillDurationSeconds;

    public GlobalIpRateLimitFilter(RateLimiterService rateLimiterService,
                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.rateLimiterService = rateLimiterService;
        this.resolver = resolver;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        if (!rateLimitEnabled) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI();

        // Whitelist: bypass rate limiting for login, webhooks, and static files
        if (isWhitelisted(path)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        String clientIp = ClientIpUtil.getClientIp(request);
        String key = "GLOBAL_IP:" + clientIp;

        RateLimiterService.ConsumptionResult result = rateLimiterService.tryConsume(
                key, globalCapacity, globalCapacity, globalRefillDurationSeconds
        );

        if (result.allowed()) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Blocked — delegate to GlobalExceptionHandler
        long retryAfterSeconds = Math.max(1, result.retryAfterNanos() / 1_000_000_000);
        log.warn("Global rate limit exceeded for IP [{}] on path [{}]", clientIp, path);
        
        resolver.resolveException(request, response, null, new RateLimitExceededException(
                "Too many requests from your IP. Please try again after " + retryAfterSeconds + " seconds.",
                retryAfterSeconds,
                globalCapacity
        ));
    }

    /**
     * Checks if a request path should bypass global rate limiting.
     */
    private boolean isWhitelisted(String path) {
        return path.equals("/api/auth/login")
                || path.startsWith("/api/webhook")
                || path.startsWith("/uploads/");
    }
}
