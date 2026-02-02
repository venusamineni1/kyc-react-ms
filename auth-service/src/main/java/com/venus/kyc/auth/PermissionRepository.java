package com.venus.kyc.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PermissionRepository {

    private final JdbcClient jdbcClient;

    public PermissionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<String> findPermissionsByRole(String roleName) {
        return jdbcClient.sql("SELECT Permission FROM RolePermissions WHERE RoleName = :roleName")
                .param("roleName", roleName)
                .query(String.class)
                .list();
    }

    public List<String> findAllRoles() {
        return jdbcClient.sql("SELECT DISTINCT Role FROM AppUsers")
                .query(String.class)
                .list();
    }

    public List<String> findAllPermissions() {
        return List.of(
                "VIEW_CLIENTS",
                "EDIT_CLIENTS",
                "VIEW_SENSITIVE_DATA",
                "MANAGE_USERS",
                "VIEW_CHANGES",
                "MANAGE_PERMISSIONS",
                "MANAGE_CASES",
                "APPROVE_CASES_STAGE1",
                "APPROVE_CASES_STAGE2",
                "APPROVE_CASES_STAGE3",
                "APPROVE_CASES_STAGE4",
                "MANAGE_RISK");
    }

    public void addPermissionToRole(String roleName, String permission) {
        jdbcClient.sql("INSERT INTO RolePermissions (RoleName, Permission) VALUES (:roleName, :permission)")
                .param("roleName", roleName)
                .param("permission", permission)
                .update();
    }

    public void removePermissionFromRole(String roleName, String permission) {
        jdbcClient.sql("DELETE FROM RolePermissions WHERE RoleName = :roleName AND Permission = :permission")
                .param("roleName", roleName)
                .param("permission", permission)
                .update();
    }

    public java.util.Map<String, List<String>> getAllRolePermissions() {
        return jdbcClient.sql("SELECT RoleName, Permission FROM RolePermissions")
                .query((rs, rowNum) -> java.util.Map.entry(rs.getString("RoleName"), rs.getString("Permission")))
                .list().stream()
                .collect(java.util.stream.Collectors.groupingBy(java.util.Map.Entry::getKey,
                        java.util.stream.Collectors.mapping(java.util.Map.Entry::getValue,
                                java.util.stream.Collectors.toList())));
    }
}
