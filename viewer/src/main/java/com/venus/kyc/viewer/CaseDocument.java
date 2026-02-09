package com.venus.kyc.viewer;

public record CaseDocument(
        Long documentID,
        Long caseID,
        String documentName,
        String category,
        String mimeType,
        String uploadedBy,
        String comment,
        byte[] data,
        java.time.LocalDateTime uploadDate,
        int version) {
}
