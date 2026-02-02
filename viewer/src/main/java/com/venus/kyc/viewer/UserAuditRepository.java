package com.venus.kyc.viewer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UserAuditRepository {

    private final JdbcClient jdbcClient;

    public UserAuditRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void logAction(Long userId, String username, String action, String details, String ipAddress) {
        String sql = "INSERT INTO UserAudits (UserID, Username, Action, Details, IPAddress, Timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcClient.sql(sql)
                .params(userId, username, action, details, ipAddress, LocalDateTime.now())
                .update();
    }

    public List<UserAudit> findAll() {
        return jdbcClient.sql("SELECT * FROM UserAudits ORDER BY Timestamp DESC")
                .query(UserAudit.class)
                .list();
    }
}
