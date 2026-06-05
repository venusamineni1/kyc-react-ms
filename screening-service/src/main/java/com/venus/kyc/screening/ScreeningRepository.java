package com.venus.kyc.screening;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class ScreeningRepository {

    private final JdbcClient jdbcClient;

    public ScreeningRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // ── ScreeningLogs ─────────────────────────────────────────────────────────

    public Long saveLog(ScreeningLog log) {
        String sql = """
                INSERT INTO ScreeningLogs
                  (ClientID, RequestPayload, ResponsePayload, OverallStatus, ExternalRequestID, CreatedAt, NrtsProcessId)
                VALUES (:clientId, :requestPayload, :responsePayload, :overallStatus, :externalRequestID, :createdAt, :nrtsProcessId)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(sql)
                .param("clientId", log.clientID())
                .param("requestPayload", log.requestPayload())
                .param("responsePayload", log.responsePayload())
                .param("overallStatus", log.overallStatus())
                .param("externalRequestID", log.externalRequestID())
                .param("createdAt", log.createdAt() != null ? log.createdAt() : LocalDateTime.now())
                .param("nrtsProcessId", log.nrtsProcessId())
                .update(keyHolder);

        return extractKey(keyHolder, "LOGID");
    }

    public void updateLog(Long logId, String responsePayload, String overallStatus) {
        jdbcClient.sql("""
                UPDATE ScreeningLogs
                SET ResponsePayload = :responsePayload, OverallStatus = :overallStatus
                WHERE LogID = :logId
                """)
                .param("responsePayload", responsePayload)
                .param("overallStatus", overallStatus)
                .param("logId", logId)
                .update();
    }

    /** Stores the NRTS numeric processId against the log row. */
    public void updateNrtsProcessId(Long logId, Long nrtsProcessId) {
        jdbcClient.sql("UPDATE ScreeningLogs SET NrtsProcessId = :nrtsProcessId WHERE LogID = :logId")
                .param("nrtsProcessId", nrtsProcessId)
                .param("logId", logId)
                .update();
    }

    public ScreeningLog findLogByExternalId(String externalId) {
        return jdbcClient.sql("SELECT * FROM ScreeningLogs WHERE ExternalRequestID = :externalId")
                .param("externalId", externalId)
                .query(ScreeningLog.class)
                .optional().orElse(null);
    }

    public ScreeningLog findLogByNrtsProcessId(Long nrtsProcessId) {
        return jdbcClient.sql("SELECT * FROM ScreeningLogs WHERE NrtsProcessId = :nrtsProcessId")
                .param("nrtsProcessId", nrtsProcessId)
                .query(ScreeningLog.class)
                .optional().orElse(null);
    }

    public List<ScreeningLog> findLogsByClientId(Long clientId) {
        return jdbcClient.sql("SELECT * FROM ScreeningLogs WHERE ClientID = :clientId ORDER BY CreatedAt DESC")
                .param("clientId", clientId)
                .query(ScreeningLog.class)
                .list();
    }

    // ── ScreeningResults ──────────────────────────────────────────────────────

    public Long saveResult(ScreeningResult result) {
        String sql = """
                INSERT INTO ScreeningResults
                  (ScreeningLogID, ContextType, Status, AlertStatus, AlertMessage, AlertID, NrtsReqId)
                VALUES (:screeningLogID, :contextType, :status, :alertStatus, :alertMessage, :alertID, :nrtsReqId)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(sql)
                .param("screeningLogID", result.screeningLogID())
                .param("contextType", result.contextType())
                .param("status", result.status())
                .param("alertStatus", result.alertStatus())
                .param("alertMessage", result.alertMessage())
                .param("alertID", result.alertID())
                .param("nrtsReqId", result.nrtsReqId())
                .update(keyHolder);

        return extractKey(keyHolder, "RESULTID");
    }

    /** Updates the NrtsReqId on the single result row for a log (1 client = 1 ReqId). */
    public void updateNrtsReqId(Long logId, Long nrtsReqId) {
        jdbcClient.sql("UPDATE ScreeningResults SET NrtsReqId = :nrtsReqId WHERE ScreeningLogID = :logId")
                .param("nrtsReqId", nrtsReqId)
                .param("logId", logId)
                .update();
    }

    public void deleteResultsByLogId(Long logId) {
        jdbcClient.sql("DELETE FROM ScreeningResults WHERE ScreeningLogID = :logId")
                .param("logId", logId)
                .update();
    }

    public List<ScreeningResult> findResultsByLogId(Long logId) {
        return jdbcClient.sql("SELECT * FROM ScreeningResults WHERE ScreeningLogID = :logId")
                .param("logId", logId)
                .query(ScreeningResult.class)
                .list();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long extractKey(KeyHolder keyHolder, String columnName) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null) throw new RuntimeException("No generated key returned");
        Object key = keys.get(columnName);
        if (key == null) key = keys.get(columnName.toLowerCase());
        if (key == null && !keys.isEmpty()) key = keys.values().iterator().next();
        if (key == null) throw new RuntimeException("Could not retrieve generated key");
        return ((Number) key).longValue();
    }
}
