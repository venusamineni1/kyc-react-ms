package com.venus.kyc.viewer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<User> findAll() {
        return jdbcClient.sql("SELECT * FROM Users")
                .query(User.class)
                .list();
    }

    public void save(User user) {
        jdbcClient.sql(
                "INSERT INTO Users (Username, Password, Role, Active) VALUES (:username, :password, :role, :active)")
                .param("username", user.username())
                .param("password", user.password())
                .param("role", user.role())
                .param("active", user.active())
                .update();
    }

    public void updateRole(String username, String role) {
        jdbcClient.sql("UPDATE Users SET Role = :role WHERE Username = :username")
                .param("role", role)
                .param("username", username)
                .update();
    }

    public Map<String, List<String>> findAllPermissions() {
        // 1. Get existing permissions
        Map<String, List<String>> permissionsMap = jdbcClient.sql("SELECT Role, Permission FROM RolePermissions")
                .query((rs, rowNum) -> Map.entry(rs.getString("Role"), rs.getString("Permission")))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        // 2. Get all roles used by Users (some might not have permissions yet)
        List<String> userRoles = jdbcClient.sql("SELECT DISTINCT Role FROM Users")
                .query(String.class)
                .list();

        // 3. Ensure all user roles are in the map
        for (String role : userRoles) {
            permissionsMap.putIfAbsent(role, new java.util.ArrayList<>());
        }

        ensureSystemRoles(permissionsMap);
        System.out.println("DEBUG: findAllPermissions returning keys: " + permissionsMap.keySet());
        return permissionsMap;
    }

    private void ensureSystemRoles(Map<String, List<String>> permissionsMap) {
        List<String> systemRoles = List.of(
                "ADMIN",
                "KYC_ANALYST", "KYC_REVIEWER", "AFC_REVIEWER", "ACO_REVIEWER", "AUDITOR",
                "ROLE_ANALYST", "ROLE_REVIEWER", "ROLE_ACO_REVIEWER");
        for (String role : systemRoles) {
            permissionsMap.putIfAbsent(role, new java.util.ArrayList<>());
        }
    }

    public List<String> findAllAvailablePermissions() {
        // In a real app this might be an enum or table, here we just select distinct
        // from what we have + maybe some hardcoded?
        // For now, let's return a hardcoded list of known permissions so the UI has
        // options
        return List.of(
                "MANAGE_USERS", "MANAGE_PERMISSIONS", "MANAGE_CASES", "VIEW_CLIENTS", "VIEW_CHANGES",
                "ROLE_AFC_REVIEWER", "MANAGE_RISK", "MANAGE_SCREENING", "MANAGE_CLIENTS", "MANAGE_AUDITS",
                "MANAGE_CONFIG", "ROLE_ANALYST", "ROLE_REVIEWER", "ROLE_ACO_REVIEWER");
    }

    public void updatePermissions(String role, List<String> permissions) {
        // Delete existing
        jdbcClient.sql("DELETE FROM RolePermissions WHERE Role = :role")
                .param("role", role)
                .update();

        // Insert new
        for (String perm : permissions) {
            jdbcClient.sql("INSERT INTO RolePermissions (Role, Permission) VALUES (:role, :perm)")
                    .param("role", role)
                    .param("perm", perm)
                    .update();
        }
    }

    public java.util.Optional<User> findByUsername(String username) {
        return jdbcClient.sql("SELECT * FROM Users WHERE Username = :username")
                .param("username", username)
                .query(User.class)
                .optional();
    }

    public List<String> findPermissionsByRole(String role) {
        return jdbcClient.sql("SELECT Permission FROM RolePermissions WHERE Role = :role")
                .param("role", role)
                .query(String.class)
                .list();
    }

    public List<User> findByRole(String role) {
        return jdbcClient.sql("SELECT * FROM Users WHERE Role = :role")
                .param("role", role)
                .query(User.class)
                .list();
    }
}
