package com.autohub.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bypasses JwtFilter entirely for webhook paths.
 *
 * Problem: JwtFilter runs before Spring Security's authorization rules.
 * Even though we have .requestMatchers("/api/webhook/**").permitAll()
 * in SecurityConfig, JwtFilter still intercepts the request first and
 * rejects it with 401/403 because Meta's server-to-server calls don't
 * carry an Authorization header.
 *
 * Solution: Wrap JwtFilter in this bypass filter. For webhook paths,
 * we skip JwtFilter entirely and call chain.doFilter() directly.
 * For all other paths, we delegate to JwtFilter as normal.
 *
 * This is the correct pattern for mixing JWT-secured APIs with
 * public server-to-server callback endpoints in the same Spring Boot app.
 */
public class WebhookBypassFilter extends OncePerRequestFilter {

    private final JwtFilter jwtFilter;

    public WebhookBypassFilter(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip JwtFilter for webhook paths — Meta sends no Authorization header
        if (path.startsWith("/api/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        // All other paths go through JwtFilter as normal
        jwtFilter.doFilter(request, response, filterChain);
    }
}
