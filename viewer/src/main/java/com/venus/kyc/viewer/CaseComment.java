package com.venus.kyc.viewer;

import java.time.LocalDateTime;

public record CaseComment(
        Long commentID,
        Long caseID,
        String userID,
        String commentText,
        LocalDateTime commentDate,
        String role) {
}
