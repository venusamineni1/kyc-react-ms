package com.venus.kyc.viewer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CaseRepository {

    private final JdbcClient jdbcClient;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final EncryptionService enc;

    public CaseRepository(JdbcClient jdbcClient, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                          EncryptionService enc) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
        this.enc = enc;
    }

    /** Decrypt first+last individually then join — the DB columns hold AES ciphertext. */
    private Case mapCase(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        String firstName = enc.decrypt(rs.getString("FirstName"));
        String lastName  = enc.decrypt(rs.getString("LastName"));
        return new Case(
                rs.getLong("CaseID"),
                rs.getLong("ClientID"),
                firstName + " " + lastName,
                rs.getObject("CreatedDate", LocalDateTime.class),
                rs.getString("Reason"),
                rs.getString("AssignedTo"),
                rs.getString("Status"),
                rs.getString("InstanceID"),
                rs.getString("WorkflowType")
        );
    }

    private static final String CASE_SELECT = """
            SELECT c.CaseID, c.ClientID, cl.FirstName, cl.LastName,
                   c.CreatedDate, c.Reason, c.AssignedTo, c.Status, c.InstanceID, c.WorkflowType
            FROM Cases c
            JOIN Clients cl ON c.ClientID = cl.ClientID
            """;

    public List<Case> findAll() {
        return jdbcClient.sql(CASE_SELECT)
                .query(this::mapCase)
                .list();
    }

    public List<Case> findByClientId(Long clientID) {
        return jdbcClient.sql(CASE_SELECT + " WHERE c.ClientID = :clientID ORDER BY c.CreatedDate DESC")
                .param("clientID", clientID)
                .query(this::mapCase)
                .list();
    }

    public Optional<Case> findById(Long id) {
        return jdbcClient.sql(CASE_SELECT + " WHERE c.CaseID = :id")
                .param("id", id)
                .query(this::mapCase)
                .optional();
    }

    public List<CaseComment> findCommentsByCaseId(Long caseId) {
        return jdbcClient.sql("SELECT * FROM CaseComments WHERE CaseID = :caseId ORDER BY CommentDate ASC")
                .param("caseId", caseId)
                .query(CaseComment.class)
                .list();
    }

    public void updateStatus(Long id, String status, String assignedTo) {
        if (status != null) {
            // Update status and assignee together (assignee could be null to clear it)
            jdbcTemplate.update("UPDATE Cases SET Status = ?, AssignedTo = ? WHERE CaseID = ?", status, assignedTo, id);
        } else {
            // Update only assignee (could be null to clear it)
            jdbcTemplate.update("UPDATE Cases SET AssignedTo = ? WHERE CaseID = ?", assignedTo, id);
        }
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

    public List<DocumentAnnotation> findAnnotationsByDocumentId(Long docId) {
        return jdbcClient
                .sql("SELECT * FROM DocumentAnnotations WHERE DocumentID = :docId ORDER BY CreatedDate ASC")
                .param("docId", docId)
                .query(DocumentAnnotation.class)
                .list();
    }

    public void addDocumentAnnotation(Long docId, Long caseId, String userId,
                                       String text, String geometry, String label) {
        jdbcClient.sql("""
                INSERT INTO DocumentAnnotations
                  (DocumentID, CaseID, UserID, AnnotationText, Geometry, Label)
                VALUES (:docId, :caseId, :userId, :text, :geometry, :label)
                """)
                .param("docId", docId)
                .param("caseId", caseId)
                .param("userId", userId)
                .param("text", text)
                .param("geometry", geometry)
                .param("label", label)
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
