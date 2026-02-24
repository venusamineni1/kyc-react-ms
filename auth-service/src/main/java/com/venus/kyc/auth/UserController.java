package com.venus.kyc.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for managing user accounts, passwords, and roles")
public class UserController {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserAuditService userAuditService;

    public UserController(UserRepository userRepository, PermissionRepository permissionRepository,
            UserAuditService userAuditService) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.userAuditService = userAuditService;
    }

    @Operation(summary = "Get all users", description = "Returns a list of all registered users in the system")
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Operation(summary = "Get users by role", description = "Returns users filtered by the specified role")
    @GetMapping("/role/{role}")
    public List<User> getUsersByRole(@Parameter(description = "Role name to filter by") @PathVariable String role) {
        return userRepository.findByRole(role);
    }

    @Operation(summary = "Create a new user", description = "Creates a new user account with the specified username, password, and role")
    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody CreateUserRequest request, java.security.Principal principal) {
        String password = request.password();
        if (!password.startsWith("{noop}")) {
            password = "{noop}" + password;
        }

        User newUser = new User(null, request.username(), password, request.role(), true);
        userRepository.create(newUser);

        userAuditService.log(principal != null ? principal.getName() : "system", "CREATE_USER",
                "Created user: " + request.username() + " with role " + request.role());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update password", description = "Allows the authenticated user to change their password by providing the old and new passwords")
    @PostMapping("/password")
    public ResponseEntity<String> updatePassword(
            @RequestBody PasswordChangeRequest request,
            java.security.Principal principal) {

        String currentUsername = principal.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String storedPassword = currentUser.password();
        String oldPasswordToCheck = request.oldPassword();
        if (!oldPasswordToCheck.startsWith("{noop}")) {
            oldPasswordToCheck = "{noop}" + oldPasswordToCheck;
        }

        if (!storedPassword.equals(oldPasswordToCheck)) {
            return ResponseEntity.badRequest().body("Incorrect old password");
        }

        String newPassword = request.newPassword();
        if (!newPassword.startsWith("{noop}")) {
            newPassword = "{noop}" + newPassword;
        }

        userRepository.updatePassword(currentUsername, newPassword);
        userAuditService.log(currentUsername, "UPDATE_PASSWORD", "User updated their password");
        return ResponseEntity.ok("Password updated");
    }

    @Operation(summary = "Update user role", description = "Changes the role assigned to a specific user")
    @PutMapping("/{username}/role")
    public ResponseEntity<Void> updateRole(@Parameter(description = "Username") @PathVariable String username,
            @RequestBody RoleUpdateRequest request,
            java.security.Principal principal) {
        userRepository.updateRole(username, request.role());
        userAuditService.log(principal != null ? principal.getName() : "system", "UPDATE_ROLE",
                "Updated role for user " + username + " to " + request.role());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get current user info", description = "Returns the profile information and permissions of the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser(java.security.Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<String> permissions = permissionRepository.findPermissionsByRole(user.role());
        return ResponseEntity.ok(new UserInfo(user.username(), user.role(), permissions));
    }

    public record CreateUserRequest(String username, String password, String role) {
    }

    public record PasswordChangeRequest(String oldPassword, String newPassword) {
    }

    public record RoleUpdateRequest(String role) {
    }

    public record UserInfo(String username, String role, List<String> permissions) {
    }
}
