package com.venus.kyc.viewer.screening;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ScreeningRepository {

    private final JdbcClient jdbcClient;

    public ScreeningRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Long saveLog(Long clientId, String requestPayload, String responsePayload, String overallStatus, String externalRequestID) {
        return saveLog(clientId, requestPayload, responsePayload, overallStatus, externalRequestID, "MANUAL");
    }

    public Long saveLog(Long clientId, String requestPayload, String responsePayload, String overallStatus, String externalRequestID, String sourceType) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(
                "INSERT INTO ScreeningLogs (ClientID, RequestPayload, ResponsePayload, OverallStatus, ExternalRequestID, SourceType, CreatedAt) VALUES (:clientID, :requestPayload, :responsePayload, :overallStatus, :externalRequestID, :sourceType, :createdAt)")
                .param("clientID", clientId)
                .param("requestPayload", requestPayload)
                .param("responsePayload", responsePayload)
                .param("overallStatus", overallStatus)
                .param("externalRequestID", externalRequestID)
                .param("sourceType", sourceType)
                .param("createdAt", LocalDateTime.now())
                .update(keyHolder);
        return ((Number) keyHolder.getKeys().get("LOGID")).longValue();
    }

    public Long saveResult(Long logId, String contextType, String status, String hitContext) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(
                "INSERT INTO ScreeningResults (ScreeningLogID, ContextType, Status, AlertMessage) VALUES (:logID, :contextType, :status, :alertMessage)")
                .param("logID", logId)
                .param("contextType", contextType)
                .param("status", status)
                .param("alertMessage", hitContext)
                .update(keyHolder);
        return ((Number) keyHolder.getKeys().get("RESULTID")).longValue();
    }

    public ScreeningLog getLatestScreeningLog(Long clientId) {
        return jdbcClient.sql(
                "SELECT LogID, ClientID, RequestPayload, ResponsePayload, OverallStatus, ExternalRequestID, SourceType, CreatedAt FROM ScreeningLogs WHERE ClientID = :clientID ORDER BY CreatedAt DESC LIMIT 1")
                .param("clientID", clientId)
                .query((rs, rowNum) -> new ScreeningLog(
                        rs.getLong("LogID"),
                        rs.getLong("ClientID"),
                        rs.getString("RequestPayload"),
                        rs.getString("ResponsePayload"),
                        rs.getString("OverallStatus"),
                        rs.getString("ExternalRequestID"),
                        rs.getString("SourceType"),
                        rs.getObject("CreatedAt", java.time.LocalDateTime.class)
                ))
                .optional()
                .orElse(null);
    }

    public List<ScreeningLog> getHistory(Long clientId) {
        return jdbcClient.sql(
                "SELECT LogID, ClientID, RequestPayload, ResponsePayload, OverallStatus, ExternalRequestID, SourceType, CreatedAt FROM ScreeningLogs WHERE ClientID = :clientID ORDER BY CreatedAt DESC")
                .param("clientID", clientId)
                .query((rs, rowNum) -> new ScreeningLog(
                        rs.getLong("LogID"),
                        rs.getLong("ClientID"),
                        rs.getString("RequestPayload"),
                        rs.getString("ResponsePayload"),
                        rs.getString("OverallStatus"),
                        rs.getString("ExternalRequestID"),
                        rs.getString("SourceType"),
                        rs.getObject("CreatedAt", java.time.LocalDateTime.class)
                ))
                .list();
    }

    public List<ScreeningResult> getResultsByLogId(Long logId) {
        return jdbcClient.sql(
                "SELECT ResultID, ScreeningLogID, ContextType, Status, AlertStatus, AlertMessage, AlertID FROM ScreeningResults WHERE ScreeningLogID = :logID")
                .param("logID", logId)
                .query((rs, rowNum) -> new ScreeningResult(
                        rs.getLong("ResultID"),
                        rs.getLong("ScreeningLogID"),
                        rs.getString("ContextType"),
                        rs.getString("Status"),
                        rs.getString("AlertStatus"),
                        rs.getString("AlertMessage"),
                        rs.getString("AlertID")
                ))
                .list();
    }
}
