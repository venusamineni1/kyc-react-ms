package com.venus.kyc.viewer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public ResponseEntity<Void> createUser(@RequestBody User user) {
        // Simple create, assume password is raw for demo
        userRepository.save(new User(user.username(), user.password(), user.role(), true, null));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{username}/role")
    public ResponseEntity<Void> updateUserRole(@PathVariable String username,
            @RequestBody Map<String, String> payload) {
        String role = payload.get("role");
        userRepository.updateRole(username, role);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/permissions")
    public Map<String, List<String>> getPermissions() {
        return userRepository.findAllPermissions();
    }

    @GetMapping("/permissions/all")
    public List<String> getAllPermissions() {
        return userRepository.findAllAvailablePermissions();
    }

    @PostMapping("/permissions/role/{role}")
    public ResponseEntity<Void> updateRolePermissions(@PathVariable String role,
            @RequestBody Map<String, List<String>> payload) {
        List<String> permissions = payload.get("permissions");
        userRepository.updatePermissions(role, permissions);
        return ResponseEntity.ok().build();
    }
}
