package com.venus.kyc.viewer;

import java.time.LocalDateTime;

public record DocumentAnnotation(
        Long annotationID,
        Long documentID,
        Long caseID,
        String userID,
        String annotationText,
        String geometry,
        String label,
        LocalDateTime createdDate) {
}
