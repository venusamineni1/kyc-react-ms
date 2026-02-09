package com.venus.kyc.viewer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CaseRepository {

    private final JdbcClient jdbcClient;

    public CaseRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Case> findAll() {
        return jdbcClient.sql("""
                SELECT c.CaseID, c.ClientID, cl.FirstName || ' ' || cl.LastName as clientName,
                       c.CreatedDate, c.Reason, c.AssignedTo, c.Status, c.InstanceID, c.WorkflowType
                FROM Cases c
                JOIN Clients cl ON c.ClientID = cl.ClientID
                """)
                .query(Case.class)
                .list();
    }

    public List<Case> findByClientId(Long clientID) {
        return jdbcClient.sql("""
                SELECT c.CaseID, c.ClientID, cl.FirstName || ' ' || cl.LastName as clientName,
                       c.CreatedDate, c.Reason, c.AssignedTo, c.Status, c.InstanceID, c.WorkflowType
                FROM Cases c
                JOIN Clients cl ON c.ClientID = cl.ClientID
                WHERE c.ClientID = :clientID
                ORDER BY c.CreatedDate DESC
                """)
                .param("clientID", clientID)
                .query(Case.class)
                .list();
    }

    public Optional<Case> findById(Long id) {
        return jdbcClient.sql("""
                SELECT c.CaseID, c.ClientID, cl.FirstName || ' ' || cl.LastName as clientName,
                       c.CreatedDate, c.Reason, c.AssignedTo, c.Status, c.InstanceID, c.WorkflowType
                FROM Cases c
                JOIN Clients cl ON c.ClientID = cl.ClientID
                WHERE c.CaseID = :id
                """)
                .param("id", id)
                .query(Case.class)
                .optional();
    }

    public List<CaseComment> findCommentsByCaseId(Long caseId) {
        return jdbcClient.sql("SELECT * FROM CaseComments WHERE CaseID = :caseId ORDER BY CommentDate ASC")
                .param("caseId", caseId)
                .query(CaseComment.class)
                .list();
    }

    public void updateStatus(Long id, String status, String assignedTo) {
        jdbcClient
                .sql("UPDATE Cases SET Status = COALESCE(:status, Status), AssignedTo = :assignedTo WHERE CaseID = :id")
                .param("status", status)
                .param("assignedTo", assignedTo)
                .param("id", id)
                .update();
    }

    public void addComment(Long caseId, String userId, String text, String role) {
        jdbcClient.sql(
                "INSERT INTO CaseComments (CaseID, UserID, CommentText, Role) VALUES (:caseId, :userId, :text, :role)")
                .param("caseId", caseId)
                .param("userId", userId)
                .param("text", text)
                .param("role", role)
                .update();
    }

    public Long create(Long clientID, String reason, String status, String assignedTo, String instanceId,
            String workflowType) {
        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO Cases (ClientID, Reason, Status, AssignedTo, InstanceID, WorkflowType)
                VALUES (:clientID, :reason, :status, :assignedTo, :instanceId, :workflowType)
                """)
                .param("clientID", clientID)
                .param("reason", reason)
                .param("status", status)
                .param("assignedTo", assignedTo)
                .param("instanceId", instanceId)
                .param("workflowType", workflowType)
                .update(keyHolder, "CaseID");
        return keyHolder.getKey().longValue();
    }

    public void updateInstanceInfo(Long id, String instanceId, String workflowType) {
        jdbcClient.sql("UPDATE Cases SET InstanceID = :instanceId, WorkflowType = :workflowType WHERE CaseID = :id")
                .param("instanceId", instanceId)
                .param("workflowType", workflowType)
                .param("id", id)
                .update();
    }
}
