package com.venus.kyc.viewer;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AuthController {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    private final UserRepository userRepository;
    private final UserAuditService userAuditService;

    public AuthController(UserRepository userRepository, UserAuditService userAuditService) {
        this.userRepository = userRepository;
        this.userAuditService = userAuditService;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String username = request.username();
        String password = request.password();

        // Fetch user from DB
        var userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        var user = userOpt.get();

        // Validate password (plaintext for demo/dev as per requirement)
        if (!password.equals(user.password())) {
            // Optional: Log failed login attempt
            // userAuditService.log(username, "LOGIN_FAILED", "Invalid password");
            return ResponseEntity.status(401).build();
        }

        if (!user.active()) {
            return ResponseEntity.status(403).body(Map.of("error", "User is inactive"));
        }

        // Fetch permissions
        var permissions = userRepository.findPermissionsByRole(user.role());

        // Construct authorities list
        // Add scope ROLE_{ROLE_NAME} and individual permissions
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.role()));

        for (String perm : permissions) {
            authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(perm));
        }

        String token = generateToken(username, authorities);

        // Audit Log
        userAuditService.log(username, "LOGIN", "User logged in successfully");

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", token);
        response.put("username", username);
        response.put("role", authorities.stream().filter(a -> a.getAuthority().startsWith("ROLE_")).findFirst()
                .map(GrantedAuthority::getAuthority).orElse("User"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(org.springframework.security.core.Authentication authentication) {
        if (authentication != null) {
            userAuditService.log(authentication.getName(), "LOGOUT", "User logged out");
        }
        return ResponseEntity.ok().build();
    }

    private String generateToken(String username, List<GrantedAuthority> authorities) {
        String authString = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(username)
                .claim("auth", authString)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours
                .signWith(key)
                .compact();
    }

    public record LoginRequest(String username, String password) {
    }
}
