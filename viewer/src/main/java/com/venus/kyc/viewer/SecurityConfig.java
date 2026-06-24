package com.venus.kyc.viewer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests((authorize) -> authorize
                                                .requestMatchers("/", "/index.html", "/login.html", "/login",
                                                                "/api/auth/login", "/api/v1/auth/login",
                                                                "/style.css",
                                                                "/assets/**",
                                                                "/*.js", "/*.css", "/*.ico", "/*.png", "/*.jpg",
                                                                "/api/risk/**", "/api/v1/risk/**",
                                                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                                .permitAll()
                                                // APIs that moved to auth-service will be routed by Gateway, but if
                                                // someone calls viewer directly:
                                                .requestMatchers("/api/users/me", "/api/v1/users/me").authenticated()
                                                .requestMatchers("/api/users/role/**", "/api/v1/users/role/**").authenticated()
                                                .requestMatchers("/api/users/**", "/api/v1/users/**").hasAuthority("MANAGE_USERS")
                                                .requestMatchers("/api/permissions/**", "/api/v1/permissions/**")
                                                .hasAuthority("MANAGE_PERMISSIONS")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/clients", "/api/v1/clients")
                                                .authenticated()
                                                .requestMatchers("/api/clients/**", "/api/v1/clients/**").hasAuthority("VIEW_CLIENTS")
                                                .requestMatchers("/api/clients/changes", "/api/v1/clients/changes").hasAuthority("VIEW_CHANGES")
                                                .requestMatchers("/api/cases/**", "/api/v1/cases/**").hasAuthority("MANAGE_CASES")
                                                .requestMatchers("/api/admin/audits", "/api/v1/admin/audits").hasAnyAuthority("MANAGE_AUDITS", "VIEW_CLIENTS")
                                                .requestMatchers("/api/admin/config", "/api/v1/admin/config").hasAuthority("MANAGE_CONFIG")
                                                .requestMatchers("/api/internal/scheduler/**", "/api/v1/internal/scheduler/**").hasAuthority("MANAGE_CONFIG")
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .exceptionHandling(e -> e
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

                return http.build();
        }
}
