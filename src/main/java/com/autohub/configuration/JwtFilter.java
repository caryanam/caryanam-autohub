package com.autohub.configuration;

import java.io.IOException;

import com.autohub.serviceImpl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {


    private final JwtUtil jwtUtil;

    private final CustomUserDetailsService userDetailsService;

    /**
     * Skip JwtFilter entirely for webhook paths.
     * Meta's server-to-server webhook calls don't carry an Authorization header.
     * Without this override, Spring Boot auto-registers this @Component filter
     * in the servlet filter chain independently of WebhookBypassFilter,
     * causing it to run on webhook requests even though WebhookBypassFilter
     * tries to skip it inside the Spring Security chain.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();


        if (
                path.startsWith("/api/auth/login") ||
                        path.startsWith("/api/auth/logout")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            if (AuthServiceImpl.isTokenBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token expired (Logged out)");
                return;
            }

            try {
                String username = jwtUtil.extractUsername(token);

                if (username != null &&
                        SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails =
                            userDetailsService.loadUserByUsername(username);

                    if (jwtUtil.validateToken(token, userDetails.getUsername())) {

                        //  IMPORTANT
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,   // ✅ CustomUserDetails object
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authToken);

                    }
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Token expired\"}");
                return;
            } catch (io.jsonwebtoken.JwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Invalid token\"}");
                return;
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"User not found or username changed. Please login again.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);

    }

}