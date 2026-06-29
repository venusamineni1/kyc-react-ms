package com.venus.kyc.screening;

import com.venus.kyc.screening.crypto.PiiCryptoService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Append-only audit trail of every real request/response exchanged with NRTS. Unlike
 * ScreeningRepository (which tracks current/latest state), every call here is an INSERT —
 * rows are never updated or overwritten, so the full interaction history for a screening
 * request is always queryable.
 */
@Repository
public class ScreeningInteractionRepository {

    private final JdbcClient jdbcClient;
    private final PiiCryptoService crypto;

    public ScreeningInteractionRepository(JdbcClient jdbcClient, PiiCryptoService crypto) {
        this.jdbcClient = jdbcClient;
        this.crypto = crypto;
    }

    public Long saveInteraction(ScreeningNrtsInteraction interaction) {
        String sql = """
                INSERT INTO ScreeningNrtsInteractions
                  (ScreeningLogID, ClientID, ExternalRequestID, NrtsProcessId, NrtsReqId,
                   InteractionType, HttpStatus, RequestPayload, ResponsePayload, IsFinal, CreatedAt)
                VALUES (:screeningLogID, :clientID, :externalRequestID, :nrtsProcessId, :nrtsReqId,
                        :interactionType, :httpStatus, :requestPayload, :responsePayload, :isFinal, :createdAt)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(sql)
                .param("screeningLogID", interaction.screeningLogID())
                .param("clientID", interaction.clientID())
                .param("externalRequestID", interaction.externalRequestID())
                .param("nrtsProcessId", interaction.nrtsProcessId())
                .param("nrtsReqId", interaction.nrtsReqId())
                .param("interactionType", interaction.interactionType())
                .param("httpStatus", interaction.httpStatus())
                .param("requestPayload", crypto.encrypt(interaction.requestPayload()))
                .param("responsePayload", crypto.encrypt(interaction.responsePayload()))
                .param("isFinal", interaction.isFinal() != null ? interaction.isFinal() : Boolean.FALSE)
                .param("createdAt", interaction.createdAt() != null ? interaction.createdAt() : LocalDateTime.now())
                .update(keyHolder);

        return extractKey(keyHolder, "INTERACTIONID");
    }

    public List<ScreeningNrtsInteraction> findInteractionsByLogId(Long screeningLogId) {
        return decryptAll(jdbcClient.sql(
                "SELECT * FROM ScreeningNrtsInteractions WHERE ScreeningLogID = :logId ORDER BY CreatedAt")
                .param("logId", screeningLogId)
                .query(ScreeningNrtsInteraction.class)
                .list());
    }

    public List<ScreeningNrtsInteraction> findInteractionsByProcessId(Long nrtsProcessId) {
        return decryptAll(jdbcClient.sql(
                "SELECT * FROM ScreeningNrtsInteractions WHERE NrtsProcessId = :processId ORDER BY CreatedAt")
                .param("processId", nrtsProcessId)
                .query(ScreeningNrtsInteraction.class)
                .list());
    }

    private List<ScreeningNrtsInteraction> decryptAll(List<ScreeningNrtsInteraction> rows) {
        return rows.stream().map(r -> new ScreeningNrtsInteraction(
                r.interactionID(), r.screeningLogID(), r.clientID(), r.externalRequestID(),
                r.nrtsProcessId(), r.nrtsReqId(), r.interactionType(), r.httpStatus(),
                crypto.decrypt(r.requestPayload()), crypto.decrypt(r.responsePayload()),
                r.isFinal(), r.createdAt()
        )).toList();
    }

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
