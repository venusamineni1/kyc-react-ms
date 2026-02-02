package com.venus.kyc.viewer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CaseEventRepository {

    private final JdbcClient jdbcClient;

    public CaseEventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void addEvent(Long caseId, String type, String description, String source) {
        jdbcClient.sql("INSERT INTO CaseEvents (CaseID, EventType, EventDescription, EventSource) VALUES (?, ?, ?, ?)")
                .param(caseId)
                .param(type)
                .param(description)
                .param(source)
                .update();
    }

    public List<CaseEvent> findEventsByCaseId(Long caseId) {
        return jdbcClient.sql("SELECT * FROM CaseEvents WHERE CaseID = ? ORDER BY EventDate DESC")
                .param(caseId)
                .query(CaseEvent.class)
                .list();
    }
}
