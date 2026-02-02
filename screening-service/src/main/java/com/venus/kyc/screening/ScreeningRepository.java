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

    public Long saveLog(ScreeningLog log) {
        String sql = "INSERT INTO ScreeningLogs (ClientID, RequestPayload, ResponsePayload, OverallStatus, ExternalRequestID, CreatedAt) VALUES (:clientId, :requestPayload, :responsePayload, :overallStatus, :externalRequestID, :createdAt)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(sql)
                .param("clientId", log.clientID())
                .param("requestPayload", log.requestPayload())
                .param("responsePayload", log.responsePayload())
                .param("overallStatus", log.overallStatus())
                .param("externalRequestID", log.externalRequestID())
                .param("createdAt", log.createdAt() != null ? log.createdAt() : LocalDateTime.now())
                .update(keyHolder);

        // H2 returns generated keys.
        Map<String, Object> keys = keyHolder.getKeys();
        Object key = null;
        if (keys != null) {
            key = keys.get("LogID");
            if (key == null)
                key = keys.get("LOGID");
            if (key == null)
                key = keys.get("logid");
        }

        if (key == null) {
            // throw new RuntimeException("Could not retrieve LOGID from generated keys: " +
            // (keys != null ? keys.keySet() : "null"));
            // H2 might behave differently in MEM mode sometimes, but usually
            // GeneratedKeyHolder works.
            // If key is null here, something is wrong with schema or driver.
            // Let's assume it works as in risk-service.
            if (keys.values().size() > 0)
                key = keys.values().iterator().next();
        }

        return ((Number) key).longValue();
    }

    public void updateLog(Long logId, String responsePayload, String overallStatus) {
        String sql = "UPDATE ScreeningLogs SET ResponsePayload = :responsePayload, OverallStatus = :overallStatus WHERE LogID = :logId";
        jdbcClient.sql(sql)
                .param("responsePayload", responsePayload)
                .param("overallStatus", overallStatus)
                .param("logId", logId)
                .update();
    }

    public Long saveResult(ScreeningResult result) {
        String sql = "INSERT INTO ScreeningResults (ScreeningLogID, ContextType, Status, AlertStatus, AlertMessage, AlertID) VALUES (:screeningLogID, :contextType, :status, :alertStatus, :alertMessage, :alertID)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(sql)
                .param("screeningLogID", result.screeningLogID())
                .param("contextType", result.contextType())
                .param("status", result.status())
                .param("alertStatus", result.alertStatus())
                .param("alertMessage", result.alertMessage())
                .param("alertID", result.alertID())
                .update(keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        Object key = null;

        if (keys != null) {
            key = keys.get("ResultID");
            if (key == null)
                key = keys.get("RESULTID");
            if (key == null)
                key = keys.get("resultid");
            if (key == null && keys.values().size() > 0)
                key = keys.values().iterator().next();
        }

        return ((Number) key).longValue();
    }

    public void deleteResultsByLogId(Long logId) {
        String sql = "DELETE FROM ScreeningResults WHERE ScreeningLogID = :logId";
        jdbcClient.sql(sql).param("logId", logId).update();
    }

    public List<ScreeningLog> findLogsByClientId(Long clientId) {
        return jdbcClient.sql("SELECT * FROM ScreeningLogs WHERE ClientID = :clientId ORDER BY CreatedAt DESC")
                .param("clientId", clientId)
                .query(ScreeningLog.class)
                .list();
    }

    public ScreeningLog findLogByExternalId(String externalId) {
        return jdbcClient.sql("SELECT * FROM ScreeningLogs WHERE ExternalRequestID = :externalId")
                .param("externalId", externalId)
                .query(ScreeningLog.class)
                .optional().orElse(null);
    }

    public List<ScreeningResult> findResultsByLogId(Long logId) {
        return jdbcClient.sql("SELECT * FROM ScreeningResults WHERE ScreeningLogID = :logId")
                .param("logId", logId)
                .query(ScreeningResult.class)
                .list();
    }
}
