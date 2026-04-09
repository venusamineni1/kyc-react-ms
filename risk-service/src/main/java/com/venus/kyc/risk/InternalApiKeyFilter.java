package com.venus.kyc.risk;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the X-Internal-Api-Key header on all /api/internal/** requests.
 * Rejects requests without a valid key with 403 Forbidden.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyFilter.class);
    private static final String HEADER = "X-Internal-Api-Key";

    @Value("${internal.api.key}")
    private String expectedKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/internal/")) {
            String key = request.getHeader(HEADER);
            if (key == null || !key.equals(expectedKey)) {
                log.warn("Rejected internal request to {} — missing or invalid X-Internal-Api-Key", path);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing or invalid internal API key");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
