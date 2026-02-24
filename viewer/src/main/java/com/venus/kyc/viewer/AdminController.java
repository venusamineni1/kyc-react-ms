package com.venus.kyc.viewer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Admin Management", description = "Admin endpoints for user and permission management")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get all users", description = "Returns a list of all registered users")
    @GetMapping("/users")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Operation(summary = "Create a user", description = "Creates a new user account")
    @PostMapping("/users")
    public ResponseEntity<Void> createUser(@RequestBody User user) {
        // Simple create, assume password is raw for demo
        userRepository.save(new User(user.username(), user.password(), user.role(), true, null));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update user role", description = "Changes the role for a specific user")
    @PutMapping("/users/{username}/role")
    public ResponseEntity<Void> updateUserRole(@Parameter(description = "Username") @PathVariable String username,
            @RequestBody Map<String, String> payload) {
        String role = payload.get("role");
        userRepository.updateRole(username, role);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get permissions map", description = "Returns all roles with their assigned permissions")
    @GetMapping("/permissions")
    public Map<String, List<String>> getPermissions() {
        return userRepository.findAllPermissions();
    }

    @Operation(summary = "Get all available permissions", description = "Returns a list of all permission strings")
    @GetMapping("/permissions/all")
    public List<String> getAllPermissions() {
        return userRepository.findAllAvailablePermissions();
    }

    @Operation(summary = "Update role permissions", description = "Replaces permissions for a specific role")
    @PostMapping("/permissions/role/{role}")
    public ResponseEntity<Void> updateRolePermissions(@Parameter(description = "Role name") @PathVariable String role,
            @RequestBody Map<String, List<String>> payload) {
        List<String> permissions = payload.get("permissions");
        userRepository.updatePermissions(role, permissions);
        return ResponseEntity.ok().build();
    }
}
