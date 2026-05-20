package com.venus.kyc.screening.batch;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Repository
public class BatchScreeningRunRepository {

    private final JdbcClient jdbcClient;

    public BatchScreeningRunRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Long save(BatchScreeningRun run) {
        String sql = """
                INSERT INTO BatchScreeningRuns
                  (RunGroupId, FileName, SystemId, TotalClientCount, TotalBatches, BatchesCompleted,
                   OverallStatus, PersistClients, CallbackWebhookUrl, ErrorMessage, CreatedAt, CompletedAt)
                VALUES
                  (:runGroupId, :fileName, :systemId, :totalClientCount, :totalBatches, :batchesCompleted,
                   :overallStatus, :persistClients, :callbackWebhookUrl, :errorMessage, :createdAt, :completedAt)
                """;

        var keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(sql)
                .param("runGroupId", run.runGroupId())
                .param("fileName", run.fileName())
                .param("systemId", run.systemId())
                .param("totalClientCount", run.totalClientCount())
                .param("totalBatches", run.totalBatches())
                .param("batchesCompleted", run.batchesCompleted() != null ? run.batchesCompleted() : 0)
                .param("overallStatus", run.overallStatus())
                .param("persistClients", run.persistClients() != null ? run.persistClients() : false)
                .param("callbackWebhookUrl", run.callbackWebhookUrl())
                .param("errorMessage", run.errorMessage())
                .param("createdAt", run.createdAt() != null ? run.createdAt() : LocalDateTime.now())
                .param("completedAt", run.completedAt())
                .update(keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        Object key = keys != null ? (keys.get("Id") != null ? keys.get("Id") : keys.get("ID")) : null;
        if (key == null && keys != null && !keys.isEmpty()) key = keys.values().iterator().next();
        return key != null ? ((Number) key).longValue() : null;
    }

    public Optional<BatchScreeningRun> findByRunGroupId(String runGroupId) {
        return jdbcClient.sql("SELECT * FROM BatchScreeningRuns WHERE RunGroupId = :runGroupId")
                .param("runGroupId", runGroupId)
                .query(BatchScreeningRun.class)
                .optional();
    }

    public Optional<BatchScreeningRun> findByFileName(String fileName) {
        return jdbcClient.sql("SELECT * FROM BatchScreeningRuns WHERE FileName = :fileName ORDER BY CreatedAt DESC LIMIT 1")
                .param("fileName", fileName)
                .query(BatchScreeningRun.class)
                .optional();
    }

    public void updateStatus(String runGroupId, String status) {
        jdbcClient.sql("UPDATE BatchScreeningRuns SET OverallStatus = :status, CompletedAt = CASE WHEN :status IN ('COMPLETED','FAILED') THEN CURRENT_TIMESTAMP ELSE NULL END WHERE RunGroupId = :runGroupId")
                .param("status", status)
                .param("runGroupId", runGroupId)
                .update();
    }

    public void incrementBatchesCompleted(String runGroupId) {
        jdbcClient.sql("UPDATE BatchScreeningRuns SET BatchesCompleted = BatchesCompleted + 1 WHERE RunGroupId = :runGroupId")
                .param("runGroupId", runGroupId)
                .update();
    }

    public void updateTotals(String runGroupId, long totalClientCount, int totalBatches) {
        jdbcClient.sql("UPDATE BatchScreeningRuns SET TotalClientCount = :totalClientCount, TotalBatches = :totalBatches WHERE RunGroupId = :runGroupId")
                .param("totalClientCount", totalClientCount)
                .param("totalBatches", totalBatches)
                .param("runGroupId", runGroupId)
                .update();
    }

    public void markFailed(String runGroupId, String errorMessage) {
        jdbcClient.sql("UPDATE BatchScreeningRuns SET OverallStatus = 'FAILED', ErrorMessage = :errorMessage, CompletedAt = CURRENT_TIMESTAMP WHERE RunGroupId = :runGroupId")
                .param("errorMessage", errorMessage)
                .param("runGroupId", runGroupId)
                .update();
    }
}
