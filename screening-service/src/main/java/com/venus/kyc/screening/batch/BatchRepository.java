package com.venus.kyc.screening.batch;

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
        String sql = "INSERT INTO BatchRuns (BatchName, RunStatus, NotificationStatus, FeedbackCount, CreatedAt, UpdatedAt, MappingSnapshotID, ClientCount) VALUES (:batchName, :runStatus, :notificationStatus, :feedbackCount, :createdAt, :updatedAt, :mappingSnapshotID, :clientCount)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(sql)
                .param("batchName", run.batchName())
                .param("runStatus", run.runStatus())
                .param("notificationStatus", run.notificationStatus())
                .param("feedbackCount", run.feedbackCount())
                .param("createdAt", run.createdAt() != null ? run.createdAt() : LocalDateTime.now())
                .param("updatedAt", run.updatedAt() != null ? run.updatedAt() : LocalDateTime.now())
                .param("mappingSnapshotID", run.mappingSnapshotID())
                .param("clientCount", run.clientCount())
                .update(keyHolder);

        return extractKey(keyHolder, "BatchID");
    }

    public void updateBatchStatus(Long batchId, String status, String notificationStatus, Integer feedbackCount) {
        String sql = "UPDATE BatchRuns SET RunStatus = :status, NotificationStatus = :notificationStatus, FeedbackCount = :feedbackCount, UpdatedAt = :updatedAt WHERE BatchID = :batchId";
        jdbcClient.sql(sql)
                .param("status", status)
                .param("notificationStatus", notificationStatus)
                .param("feedbackCount", feedbackCount)
                .param("updatedAt", LocalDateTime.now())
                .param("batchId", batchId)
                .update();
    }

    public BatchRun findByBatchName(String batchName) {
        return jdbcClient.sql("SELECT * FROM BatchRuns WHERE BatchName = :batchName")
                .param("batchName", batchName)
                .query(BatchRun.class)
                .optional().orElse(null);
    }

    public BatchRun findById(Long batchId) {
        return jdbcClient.sql("SELECT * FROM BatchRuns WHERE BatchID = :batchId")
                .param("batchId", batchId)
                .query(BatchRun.class)
                .optional().orElse(null);
    }

    public List<BatchRun> findAll() {
        return jdbcClient.sql("SELECT * FROM BatchRuns ORDER BY CreatedAt DESC")
                .query(BatchRun.class)
                .list();
    }

    public void saveError(BatchRunError error) {
        String sql = "INSERT INTO BatchRunErrors (BatchID, RecordID, ErrorCode, ErrorMessage) VALUES (:batchId, :recordId, :errorCode, :errorMessage)";
        jdbcClient.sql(sql)
                .param("batchId", error.batchID())
                .param("recordId", error.recordID())
                .param("errorCode", error.errorCode())
                .param("errorMessage", error.errorMessage())
                .update();
    }

    public void saveFeedbackResult(BatchFeedbackResult result) {
        String sql = "INSERT INTO BatchFeedbackResults (BatchID, RecordID, MatchID, MatchName, MatchScore, Status) VALUES (:batchId, :recordId, :matchId, :matchName, :matchScore, :status)";
        jdbcClient.sql(sql)
                .param("batchId", result.batchID())
                .param("recordId", result.recordID())
                .param("matchId", result.matchID())
                .param("matchName", result.matchName())
                .param("matchScore", result.matchScore())
                .param("status", result.status())
                .update();
    }

    // ── Mapping Config Snapshots ─────────────────────────────────

    public Long saveSnapshot(MappingConfigSnapshot snapshot) {
        String sql = "INSERT INTO MappingConfigSnapshots (VersionLabel, CreatedAt, CreatedBy, Source, ConfigJson) VALUES (:versionLabel, :createdAt, :createdBy, :source, :configJson)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(sql)
                .param("versionLabel", snapshot.versionLabel())
                .param("createdAt", snapshot.createdAt() != null ? snapshot.createdAt() : LocalDateTime.now())
                .param("createdBy", snapshot.createdBy())
                .param("source", snapshot.source())
                .param("configJson", snapshot.configJson())
                .update(keyHolder);
        return extractKey(keyHolder, "SnapshotID");
    }

    public MappingConfigSnapshot findSnapshotById(Long snapshotId) {
        return jdbcClient.sql("SELECT * FROM MappingConfigSnapshots WHERE SnapshotID = :id")
                .param("id", snapshotId)
                .query(MappingConfigSnapshot.class)
                .optional().orElse(null);
    }

    public MappingConfigSnapshot findLatestSnapshot() {
        return jdbcClient.sql("SELECT * FROM MappingConfigSnapshots ORDER BY CreatedAt DESC LIMIT 1")
                .query(MappingConfigSnapshot.class)
                .optional().orElse(null);
    }

    public List<MappingConfigSnapshot> findAllSnapshots() {
        return jdbcClient.sql("SELECT * FROM MappingConfigSnapshots ORDER BY CreatedAt DESC")
                .query(MappingConfigSnapshot.class)
                .list();
    }

    public long countSnapshots() {
        return jdbcClient.sql("SELECT COUNT(*) FROM MappingConfigSnapshots")
                .query(Long.class).single();
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
