package com.fahad.stayease.config;

import com.fahad.stayease.auth.service.CustomUserDetailsService;
import com.fahad.stayease.auth.JwtFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of(
                                    "status", 401,
                                    "error", "Unauthorized",
                                    "message", authException.getMessage(),
                                    "path", request.getRequestURI()
                            )));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of(
                                    "status", 403,
                                    "error", "Forbidden",
                                    "message", accessDeniedException.getMessage(),
                                    "path", request.getRequestURI()
                            )));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/info",
                                "/ping"
                        ).permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh-token").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/properties", "/api/v1/properties/*", "/api/v1/reviews/property/*").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/properties").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/properties/*").hasRole("OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/properties/*").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/properties/my-listings").hasRole("OWNER")

                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings").hasRole("RENTER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/my-bookings").hasRole("RENTER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/property/*").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/bookings/*/confirm").hasRole("OWNER")

                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews").hasRole("RENTER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/*").hasRole("RENTER")

                        .requestMatchers("/api/v1/users/profile").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/bookings/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()

                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
