package com.autohub.configuration;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        // ── Webhook endpoints — completely open ──
                        // Meta's servers call these with no Authorization header.
                        // Must be first in the chain so they're never intercepted.
                        .requestMatchers(
                                "/api/webhook",
                                "/api/webhook/**"
                        ).permitAll()

                        // ── Public endpoints ──
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/customer/**",
                                "/api/pincode/**",
                                "/api/offer-video/active",
                                "/api/festival-offer/active",
                                "/api/dealer/register",
                                "/api/dealer/register/**",
                                "/api/dealer/send-registration-otp",
                                "/api/dealer/verify-registration-otp",
                                "/api/dealer/send-whatsapp-otp",
                                "/api/dealer/verify-whatsapp-otp",
                                "/api/vehicle/dealer/**",
                                "/api/vehicle/qr/**",
                                "/api/vehicle/**",
                                "/api/vehicle/featured",
                                "/api/vehicle/latest-vehicles",
                                "/api/vehicle/non-premium/all-vehicle",
                                "/api/vehicle/premium/all-vehicle",
                                "/api/lead/generate-lead/**",
                                "/api/lead/generate-view/**",
                                "/api/lead/**",
                                "/api/dealer/dashboard/**",
                                "/uploads/**",
                                "/api/olx/**",
                                "/api/social-tracking/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()

                        // ── Authenticated endpoints ──
                        .requestMatchers(
                                "/chat/**",
                                "/api/chat/**"
                        ).authenticated()

                        // ── Admin APIs ──
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/payment/success/**",
                                "/api/payment/failed/**",
                                "/api/payment/admin/history",
                                "/api/payment/dealer/**",
                                "/api/admin/reports/**",
                                "/api/admin/all-vehicle",
                                "/api/admin/all-dealers",
                                "/api/admin/dealer/count"
                        ).hasRole("ADMIN")

                        // ── Dealer APIs ──
                        .requestMatchers(
                                "/api/lead/**",
                                "/api/dealer/**",
                                "/api/vehicle/add/**",
                                "/api/vehicle/update/**",
                                "/api/vehicle/status/**",
                                "/api/vehicle/delete/**",
                                "/api/payment/subscription/purchase",
                                "/api/analytics/**",
                                "/api/wishlist/dealer/**"
                        ).hasRole("DEALER")

                        // ── Inspector APIs ──
                        .requestMatchers(
                                "/api/inspector/**"
                        ).hasRole("INSPECTOR")

                        // ── Customer APIs ──
                        .requestMatchers(
                                "/api/lead/customer-dashboard",
                                "/api/wishlist/**"
                        ).hasRole("CUSTOMER")

                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider());

        // ── FIX: Skip JwtFilter entirely for webhook paths ──
        // JwtFilter runs before Spring Security authorization, so even
        // permitAll() doesn't stop it from rejecting requests with no
        // Authorization header. We must explicitly exclude webhook paths
        // from the filter so Meta's server-to-server calls pass through.
        http.addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://localhost:5174",
                        "https://caryanam.com",
                        "https://admin.caryanam.com",
                        "https://dealer.caryanam.com",
                        "https://c1.caryanam.com",
                        "https://caryanam-autohub-frontend.vercel.app",
                        "https://*.vercel.app"
                )
        );

        configuration.setAllowedMethods(
                Arrays.asList(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                Arrays.asList(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "Access-Control-Request-Method",
                        "Access-Control-Request-Headers"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
