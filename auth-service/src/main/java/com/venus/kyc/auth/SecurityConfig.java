package com.venus.kyc.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/**").hasAuthority("MANAGE_USERS")
                        .requestMatchers("/api/permissions/**").hasAuthority("MANAGE_PERMISSIONS")
                        .anyRequest().authenticated() // All other endpoints require auth
                )
                // Note: We are NOT using JWT Filter here for /auth/login because we issue the
                // token there.
                // But we should use it for /api/users/**
                // For now, let's keep it simple: Basic Auth or just assume internal/Gateway
                // trust?
                // BETTER: Use JWT Filter even here to validate tokens for management endpoints.
                // But I haven't implemented JwtAuthenticationFilter yet.
                // Let's implement Login only first, and assume internal traffic or basic auth
                // for now?
                // No, the Gateway will pass the Bearer token.
                // I should add a JwtAuthenticationFilter to validating incoming requests for
                // UserController.
                // For simplicity, I'll allow everything authenticated via Basic Auth (for
                // testing)
                // OR I will trust the Gateway.
                // Actually, if we use JWT, we need the Filter.
                // .httpBasic() is removed to prevent browser popup on 401
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
