package com.venus.kyc.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<User> findByUsername(String username) {
        return jdbcClient
                .sql("SELECT UserID, Username, Password, Role, Enabled FROM AppUsers WHERE Username = :username")
                .param("username", username)
                .query(User.class)
                .optional();
    }

    public List<User> findAll() {
        return jdbcClient.sql("SELECT UserID, Username, Password, Role, Enabled FROM AppUsers")
                .query(User.class)
                .list();
    }

    public List<User> findByRole(String role) {
        return jdbcClient
                .sql("SELECT UserID, Username, Password, Role, Enabled FROM AppUsers WHERE Role = :role")
                .param("role", role)
                .query(User.class)
                .list();
    }

    public void create(User user) {
        jdbcClient.sql(
                "INSERT INTO AppUsers (Username, Password, Role, Enabled) VALUES (:username, :password, :role, :enabled)")
                .param("username", user.username())
                .param("password", user.password())
                .param("role", user.role())
                .param("enabled", user.enabled())
                .update();
    }

    public void updatePassword(String username, String newPassword) {
        jdbcClient.sql("UPDATE AppUsers SET Password = :password WHERE Username = :username")
                .param("password", newPassword)
                .param("username", username)
                .update();
    }

    public void updateRole(String username, String newRole) {
        jdbcClient.sql("UPDATE AppUsers SET Role = :role WHERE Username = :username")
                .param("role", newRole)
                .param("username", username)
                .update();
    }

    // Auth service might need exists check
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
}
