package com.venus.kyc.viewer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questionnaire")
@Tag(name = "Questionnaire Management", description = "Endpoints for managing KYC questionnaire templates and case responses")
public class QuestionnaireController {

    private final QuestionnaireRepository questionnaireRepository;

    public QuestionnaireController(QuestionnaireRepository questionnaireRepository) {
        this.questionnaireRepository = questionnaireRepository;
    }

    @Operation(summary = "Get questionnaire template", description = "Returns all questionnaire sections and their questions")
    @GetMapping("/template")
    public List<QuestionnaireSection> getTemplate() {
        return questionnaireRepository.getTemplate();
    }

    @Operation(summary = "Get case responses", description = "Returns all questionnaire responses submitted for a specific case")
    @GetMapping("/case/{caseId}")
    public List<CaseQuestionnaireResponse> getResponses(@Parameter(description = "Case ID") @PathVariable Long caseId) {
        return questionnaireRepository.getResponsesForCase(caseId);
    }

    @Operation(summary = "Save case responses", description = "Saves or updates questionnaire responses for a specific case")
    @PostMapping("/case/{caseId}")
    public void saveResponses(@Parameter(description = "Case ID") @PathVariable Long caseId,
            @RequestBody List<CaseQuestionnaireResponse> responses) {
        for (CaseQuestionnaireResponse res : responses) {
            // Ensure caseID is correct
            CaseQuestionnaireResponse toSave = new CaseQuestionnaireResponse(
                    res.responseID(),
                    caseId,
                    res.questionID(),
                    res.answerText());
            questionnaireRepository.saveResponse(toSave);
        }
    }

    // Admin Template Management

    @Operation(summary = "Save template section", description = "Creates or updates a questionnaire section in the template")
    @PostMapping("/template/section")
    public void saveSection(@RequestBody QuestionnaireSection section) {
        questionnaireRepository.saveSection(section);
    }

    @Operation(summary = "Delete template section", description = "Removes a questionnaire section and its questions from the template")
    @DeleteMapping("/template/section/{id}")
    public void deleteSection(@Parameter(description = "Section ID") @PathVariable Long id) {
        questionnaireRepository.deleteSection(id);
    }

    @Operation(summary = "Save template question", description = "Creates or updates a question within a questionnaire section")
    @PostMapping("/template/question")
    public void saveQuestion(@RequestBody QuestionnaireQuestion question) {
        questionnaireRepository.saveQuestion(question);
    }

    @Operation(summary = "Delete template question", description = "Removes a question from the questionnaire template")
    @DeleteMapping("/template/question/{id}")
    public void deleteQuestion(@Parameter(description = "Question ID") @PathVariable Long id) {
        questionnaireRepository.deleteQuestion(id);
    }
}
