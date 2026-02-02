package com.venus.kyc.viewer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class QuestionnaireRepository {

    private final JdbcClient jdbcClient;

    public QuestionnaireRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<QuestionnaireSection> getTemplate() {
        List<QuestionnaireSection> sections = jdbcClient.sql(
                "SELECT * FROM QuestionnaireSections ORDER BY DisplayOrder")
                .query((rs, rowNum) -> new QuestionnaireSection(
                        rs.getLong("SectionID"),
                        rs.getString("SectionName"),
                        rs.getInt("DisplayOrder"),
                        new ArrayList<>()))
                .list();

        for (QuestionnaireSection section : sections) {
            List<QuestionnaireQuestion> questions = jdbcClient.sql(
                    "SELECT * FROM QuestionnaireQuestions WHERE SectionID = :sectionID ORDER BY DisplayOrder")
                    .param("sectionID", section.sectionID())
                    .query((rs, rowNum) -> new QuestionnaireQuestion(
                            rs.getLong("QuestionID"),
                            rs.getLong("SectionID"),
                            rs.getString("QuestionText"),
                            rs.getString("QuestionType"),
                            rs.getBoolean("IsMandatory"),
                            rs.getString("Options"),
                            rs.getInt("DisplayOrder"),
                            rs.getString("RiskFactorKey")))
                    .list();
            section.questions().addAll(questions);
        }

        return sections;
    }

    public List<CaseQuestionnaireResponse> getResponsesForCase(Long caseID) {
        return jdbcClient.sql(
                "SELECT * FROM CaseQuestionnaireResponses WHERE CaseID = :caseID")
                .param("caseID", caseID)
                .query(CaseQuestionnaireResponse.class)
                .list();
    }

    public void saveResponse(CaseQuestionnaireResponse response) {
        jdbcClient.sql(
                "MERGE INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) KEY (CaseID, QuestionID) VALUES (:caseID, :questionID, :answerText)")
                .param("caseID", response.caseID())
                .param("questionID", response.questionID())
                .param("answerText", response.answerText())
                .update();
    }

    // Admin Template Management

    public void saveSection(QuestionnaireSection section) {
        if (section.sectionID() == null) {
            jdbcClient.sql("INSERT INTO QuestionnaireSections (SectionName, DisplayOrder) VALUES (:name, :order)")
                    .param("name", section.sectionName())
                    .param("order", section.displayOrder())
                    .update();
        } else {
            jdbcClient.sql(
                    "UPDATE QuestionnaireSections SET SectionName = :name, DisplayOrder = :order WHERE SectionID = :id")
                    .param("name", section.sectionName())
                    .param("order", section.displayOrder())
                    .param("id", section.sectionID())
                    .update();
        }
    }

    public void deleteSection(Long sectionID) {
        // Delete questions first (no cascade in schema)
        jdbcClient.sql("DELETE FROM QuestionnaireQuestions WHERE SectionID = :id")
                .param("id", sectionID)
                .update();
        jdbcClient.sql("DELETE FROM QuestionnaireSections WHERE SectionID = :id")
                .param("id", sectionID)
                .update();
    }

    public void saveQuestion(QuestionnaireQuestion q) {
        if (q.questionID() == null) {
            jdbcClient.sql(
                    "INSERT INTO QuestionnaireQuestions (SectionID, QuestionText, QuestionType, IsMandatory, Options, DisplayOrder, RiskFactorKey) VALUES (:sectionID, :text, :type, :mandatory, :options, :order, :riskKey)")
                    .param("sectionID", q.sectionID())
                    .param("text", q.questionText())
                    .param("type", q.questionType())
                    .param("mandatory", q.isMandatory())
                    .param("options", q.options())
                    .param("order", q.displayOrder())
                    .param("riskKey", q.riskFactorKey())
                    .update();
        } else {
            jdbcClient.sql(
                    "UPDATE QuestionnaireQuestions SET SectionID = :sectionID, QuestionText = :text, QuestionType = :type, IsMandatory = :mandatory, Options = :options, DisplayOrder = :order, RiskFactorKey = :riskKey WHERE QuestionID = :id")
                    .param("sectionID", q.sectionID())
                    .param("text", q.questionText())
                    .param("type", q.questionType())
                    .param("mandatory", q.isMandatory())
                    .param("options", q.options())
                    .param("order", q.displayOrder())
                    .param("riskKey", q.riskFactorKey())
                    .param("id", q.questionID())
                    .update();
        }
    }

    public void deleteQuestion(Long questionID) {
        // Delete responses first
        jdbcClient.sql("DELETE FROM CaseQuestionnaireResponses WHERE QuestionID = :id")
                .param("id", questionID)
                .update();
        jdbcClient.sql("DELETE FROM QuestionnaireQuestions WHERE QuestionID = :id")
                .param("id", questionID)
                .update();
    }
}
