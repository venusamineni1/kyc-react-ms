package com.venus.kyc.viewer;

public record CaseQuestionnaireResponse(
        Long responseID,
        Long caseID,
        Long questionID,
        String answerText) {
}
