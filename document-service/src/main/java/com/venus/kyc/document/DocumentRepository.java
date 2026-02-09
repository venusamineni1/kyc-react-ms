package com.venus.kyc.document;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DocumentRepository {

    private final JdbcClient jdbcClient;

    public DocumentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Document> findByCaseId(Long caseId) {
        return jdbcClient.sql(
                "SELECT DocumentID, CaseID, DocumentName, Category, MimeType, UploadedBy, Comment, NULL as Data, UploadDate, Version FROM Documents d WHERE Version = (SELECT MAX(Version) FROM Documents d2 WHERE d2.CaseID = d.CaseID AND d2.DocumentName = d.DocumentName) AND CaseID = :caseId")
                .param("caseId", caseId)
                .query(Document.class)
                .list();
    }

    public void save(Long caseId, String name, String category, String mimeType, String uploadedBy,
            String comment, byte[] data) {
        Integer maxVersion = jdbcClient
                .sql("SELECT COALESCE(MAX(Version), 0) FROM Documents WHERE CaseID = :caseId AND DocumentName = :name")
                .param("caseId", caseId)
                .param("name", name)
                .query(Integer.class)
                .single();
        int nextVersion = maxVersion + 1;

        jdbcClient.sql(
                "INSERT INTO Documents (CaseID, DocumentName, Category, MimeType, UploadedBy, Comment, Data, Version) VALUES (:caseId, :name, :category, :mimeType, :uploadedBy, :comment, :data, :version)")
                .param("caseId", caseId)
                .param("name", name)
                .param("category", category)
                .param("mimeType", mimeType)
                .param("uploadedBy", uploadedBy)
                .param("comment", comment)
                .param("data", data)
                .param("version", nextVersion)
                .update();
    }

    public List<Document> findVersions(Long caseId, String name) {
        return jdbcClient.sql(
                "SELECT DocumentID, CaseID, DocumentName, Category, MimeType, UploadedBy, Comment, NULL as Data, UploadDate, Version FROM Documents WHERE CaseID = :caseId AND DocumentName = :name ORDER BY Version DESC")
                .param("caseId", caseId)
                .param("name", name)
                .query(Document.class)
                .list();
    }

    public Optional<Document> findById(Long id) {
        return jdbcClient.sql("SELECT * FROM Documents WHERE DocumentID = :id")
                .param("id", id)
                .query(Document.class)
                .optional();
    }
}
