package com.venus.kyc.viewer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Viewer Users", description = "Endpoints for user-related operations in the viewer application")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get users by role", description = "Returns a list of users filtered by the specified role")
    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(
            @Parameter(description = "Role name") @org.springframework.web.bind.annotation.PathVariable String role) {
        return ResponseEntity.ok(userRepository.findByRole(role));
    }

    @Operation(summary = "Get current user", description = "Returns the profile, role, and permissions of the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", authentication.getName());

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        userInfo.put("permissions", authorities);

        // Extract role (first authority starting with ROLE_)
        String role = authorities.stream()
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("USER");

        userInfo.put("role", role);

        // Include lastLogin from the database, then update it to now
        userRepository.findByUsername(authentication.getName())
                .ifPresent(u -> {
                    if (u.lastLogin() != null) {
                        userInfo.put("lastLogin", u.lastLogin().toString());
                    }
                    userRepository.updateLastLogin(authentication.getName());
                });

        return ResponseEntity.ok(userInfo);
    }
}
