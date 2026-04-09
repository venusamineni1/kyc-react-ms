package com.venus.kyc.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and JWT token generation")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserAuditService userAuditService;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                          UserAuditService userAuditService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userAuditService = userAuditService;
    }

    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns a JWT access token for subsequent API calls")
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = resolveClientIp(request);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            userAuditService.log(loginRequest.username(), "LOGIN_SUCCESS", "Login from " + ipAddress, ipAddress);
            return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
        } catch (AuthenticationException e) {
            userAuditService.log(loginRequest.username(), "LOGIN_FAILED", "Bad credentials from " + ipAddress, ipAddress);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // DTOs
    public record LoginRequest(String username, String password) {
    }

    public record JwtAuthenticationResponse(String accessToken, String tokenType) {
        public JwtAuthenticationResponse(String accessToken) {
            this(accessToken, "Bearer");
        }
    }
}
