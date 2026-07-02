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

    private final  CustomUserDetailsService userDetailsService;

    private final CustomAccessDeniedHandler accessDeniedHandler;

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;


    // ✅ Security Filter
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                                //Permit All
                                .requestMatchers(
                                        "/api/auth/**"
                                        ,"/api/customer/**"
                                        ,"/api/dealer/register/**"
                                        ,"/api/vehicle/dealer/**"
                                        ,"/api/vehicle/**"
                                        ,"/api/vehicle/featured"
                                        ,"/api/vehicle/latest-vehicles"
                                        ,"/api/vehicle/non-premium/all-vehicle"
                                        ,"/api/vehicle/premium/all-vehicle"
                                        ,"/api/lead/generate-lead/**"
                                        ,"/api/lead/generate-view/**"
                                        ,"/uploads/**"
                                        ,"/api/lead/**"
                                        ,"/api/dealer/dashboard/**"
                                        ,"/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                ).permitAll()

                                .requestMatchers(
                                        "/uploads/**","/api/olx/**")
                                .permitAll()

//                                .requestMatchers(
//                                        "/chat/**",
//                                        "/api/chat/**"
//                                ).authenticated()

                                .requestMatchers(
                                        "/chat/**",
                                        "/api/chat/**"
                                ).authenticated()


                                //ADMIN API
                                .requestMatchers("/api/admin/**"
                                        ,"/api/payment/success/**"
                                        ,"/api/payment/failed/**"
                                        ,"/api/payment/admin/history"
                                        ,"/api/payment/dealer/**"
                                        ,"/api/admin/reports/**"
                                        ,"/api/admin/all-vehicle"
                                        ,"/api/admin/all-dealers"
                                        ,"/api/admin/dealer/count"


                                ).hasRole("ADMIN")

                                //DEALER API
                                .requestMatchers(
                                        "/api/lead/**",
                                        "/api/dealer/**",
                                        "/api/vehicle/add/**",
                                        "/api/vehicle/update/**",
                                        "/api/vehicle/status/**",
                                        "/api/vehicle/delete/**",
                                        "/api/payment/subscription/purchase",
                                        "/api/analytics/**"
                                        ,"/api/wishlist/dealer/**"
                                ).hasRole("DEALER")

                                .requestMatchers("/api/lead/customer-dashboard"
                                        ,"/api/wishlist/**"
                                ).hasRole("CUSTOMER")



                                .anyRequest().authenticated()
                )

//                                //Permit All
//                                .requestMatchers(
//                                        "/api/auth/**"
//                                        ,"/api/customer/**"
//                                        ,"/api/dealer/register/**"
//                                        ,"/api/vehicle/dealer/**"
//                                        ,"/api/vehicle/**"
//                                        ,"/api/vehicle/featured"
//                                        ,"/api/vehicle/latest-vehicles"
//                                        ,"/api/vehicle/non-premium/all-vehicle"
//                                        ,"/api/vehicle/premium/all-vehicle"
//                                        ,"/api/lead/generate-lead/**"
//                                        ,"/api/lead/generate-view/**"
//                                        ,"/uploads/**"
//                                        ,"/swagger-ui/**",
//                                        "/swagger-ui.html",
//                                        "/v3/api-docs/**"
//                                ).permitAll()
//
//                                .requestMatchers(
//                                        "/uploads/**","/api/olx/**")
//                                .permitAll()
//
////                                .requestMatchers(
////                                        "/chat/**",
////                                        "/api/chat/**"
////                                ).authenticated()
//
//                                .requestMatchers(
//                                        "/chat/**",
//                                        "/api/chat/**"
//                                ).authenticated()
//
//
//                        //ADMIN API
//                                .requestMatchers("/api/admin/**"
//                                        ,"/api/payment/success/**"
//                                        ,"/api/payment/failed/**"
//                                        ,"/api/payment/admin/history"
//                                        ,"/api/payment/dealer/**"
//                                        ,"/api/admin/reports/**"
//                                        ,"/api/admin/all-vehicle"
//                                        ,"/api/admin/all-dealers"
//                                        ,"/api/admin/dealer/count"
//
//                                ).hasRole("ADMIN")
//
//                                //DEALER API
//                                .requestMatchers(
//                                        "/api/lead/**",
//                                        "/api/dealer/**",
//                                        "/api/vehicle/add/**",
//                                        "/api/vehicle/update/**",
//                                        "/api/vehicle/status/**",
//                                        "/api/vehicle/delete/**",
//                                        "/api/payment/subscription/purchase",
//                                        "/api/analytics/**"
//                                        ,"/api/wishlist/dealer/**"
//                                ).hasRole("DEALER")
//
//                                .requestMatchers("/api/lead/customer-dashboard"
//                                        ,"/api/wishlist/**"
//                                ).hasRole("CUSTOMER")
//
//
//
//                                .anyRequest().authenticated()
//                                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint) //401 Unauthorized
                        .accessDeniedHandler(accessDeniedHandler) // 403 Forbidden
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider());


        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ Authentication Manager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ✅ Authentication Provider
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    // ✅ Password Encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:63342",
                "http://127.0.0.1:5500",
                "http://localhost:5500",
                "https://caryanam.com/",
                "https://c1.caryanam.com/"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(Arrays.asList("Authorization"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;

    }


}