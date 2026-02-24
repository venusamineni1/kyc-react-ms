package com.venus.kyc.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
@Tag(name = "Permission Management", description = "Endpoints for managing role-based permissions and access control")
public class PermissionController {

    private final PermissionRepository permissionRepository;

    public PermissionController(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Operation(summary = "Get all permissions by role", description = "Returns a map of all roles and their associated permissions")
    @GetMapping
    public Map<String, List<String>> getPermissionsMap() {
        return permissionRepository.getAllRolePermissions();
    }

    @Operation(summary = "Get all roles", description = "Returns a list of all available role names")
    @GetMapping("/roles")
    public List<String> getRoles() {
        return permissionRepository.findAllRoles();
    }

    @Operation(summary = "Get all available permissions", description = "Returns a list of all permission strings available in the system")
    @GetMapping("/all")
    public List<String> getAllPermissions() {
        return permissionRepository.findAllPermissions();
    }

    @Operation(summary = "Get permissions for a role", description = "Returns the permissions assigned to a specific role")
    @GetMapping("/role/{roleName}")
    public List<String> getPermissionsForRole(@Parameter(description = "Role name") @PathVariable String roleName) {
        return permissionRepository.findPermissionsByRole(roleName);
    }

    @Operation(summary = "Update permissions for a role", description = "Replaces the entire set of permissions for a specific role")
    @PostMapping("/role/{roleName}")
    public ResponseEntity<Void> updatePermissions(@Parameter(description = "Role name") @PathVariable String roleName,
            @RequestBody Map<String, List<String>> request) {
        List<String> newPermissions = request.get("permissions");
        List<String> currentPermissions = permissionRepository.findPermissionsByRole(roleName);

        // Remove old ones
        for (String p : currentPermissions) {
            if (!newPermissions.contains(p)) {
                permissionRepository.removePermissionFromRole(roleName, p);
            }
        }

        // Add new ones
        for (String p : newPermissions) {
            if (!currentPermissions.contains(p)) {
                permissionRepository.addPermissionToRole(roleName, p);
            }
        }

        return ResponseEntity.ok().build();
    }
}
