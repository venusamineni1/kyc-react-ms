package com.venus.kyc.risk.batch;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class BatchRepository {

    private final JdbcClient jdbcClient;

    public BatchRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Long saveBatchRun(BatchRun run) {
        String sql = "INSERT INTO BatchRuns (BatchName, RunStatus, NotificationStatus, FeedbackCount, CreatedAt, UpdatedAt) VALUES (:batchName, :runStatus, :notificationStatus, :feedbackCount, :createdAt, :updatedAt)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(sql)
                .param("batchName", run.batchName())
                .param("runStatus", run.runStatus())
                .param("notificationStatus", run.notificationStatus())
                .param("feedbackCount", run.feedbackCount())
                .param("createdAt", run.createdAt() != null ? run.createdAt() : LocalDateTime.now())
                .param("updatedAt", run.updatedAt() != null ? run.updatedAt() : LocalDateTime.now())
                .update(keyHolder);

        return extractKey(keyHolder, "BatchID");
    }

    public List<BatchRun> findAll() {
        return jdbcClient.sql("SELECT * FROM BatchRuns ORDER BY CreatedAt DESC")
                .query(BatchRun.class)
                .list();
    }

    private Long extractKey(KeyHolder keyHolder, String keyName) {
        Map<String, Object> keys = keyHolder.getKeys();
        Object key = null;
        if (keys != null) {
            key = keys.get(keyName);
            if (key == null)
                key = keys.get(keyName.toUpperCase());
            if (key == null)
                key = keys.get(keyName.toLowerCase());
            if (key == null && keys.values().size() > 0)
                key = keys.values().iterator().next();
        }
        return key != null ? ((Number) key).longValue() : null;
    }
}
