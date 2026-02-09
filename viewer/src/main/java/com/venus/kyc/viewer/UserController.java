package com.venus.kyc.viewer;

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
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(
            @org.springframework.web.bind.annotation.PathVariable String role) {
        return ResponseEntity.ok(userRepository.findByRole(role));
    }

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

        return ResponseEntity.ok(userInfo);
    }
}
