package com.venus.kyc.viewer;

public record QuestionnaireQuestion(
                Long questionID,
                Long sectionID,
                String questionText,
                String questionType,
                boolean isMandatory,
                String options,
                int displayOrder,
                String riskFactorKey) {
}
