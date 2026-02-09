package com.venus.kyc.document;

import java.time.LocalDateTime;

public record Document(
                Long documentID,
                Long caseID,
                String documentName,
                String category,
                String mimeType,
                String uploadedBy,
                String comment,
                byte[] data,
                LocalDateTime uploadDate,
                int version) {
}
