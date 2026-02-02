package com.venus.kyc.viewer;

import java.util.List;

public record QuestionnaireSection(
        Long sectionID,
        String sectionName,
        int displayOrder,
        List<QuestionnaireQuestion> questions) {
}
