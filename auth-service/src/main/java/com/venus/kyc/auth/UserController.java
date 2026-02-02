package com.venus.kyc.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
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

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/role/{role}")
    public List<User> getUsersByRole(@PathVariable String role) {
        return userRepository.findByRole(role);
    }

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

    @PutMapping("/{username}/role")
    public ResponseEntity<Void> updateRole(@PathVariable String username, @RequestBody RoleUpdateRequest request,
            java.security.Principal principal) {
        userRepository.updateRole(username, request.role());
        userAuditService.log(principal != null ? principal.getName() : "system", "UPDATE_ROLE",
                "Updated role for user " + username + " to " + request.role());
        return ResponseEntity.ok().build();
    }

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
