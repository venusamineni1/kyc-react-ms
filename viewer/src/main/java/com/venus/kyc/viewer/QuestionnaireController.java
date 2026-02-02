package com.venus.kyc.viewer;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questionnaire")
public class QuestionnaireController {

    private final QuestionnaireRepository questionnaireRepository;

    public QuestionnaireController(QuestionnaireRepository questionnaireRepository) {
        this.questionnaireRepository = questionnaireRepository;
    }

    @GetMapping("/template")
    public List<QuestionnaireSection> getTemplate() {
        return questionnaireRepository.getTemplate();
    }

    @GetMapping("/case/{caseId}")
    public List<CaseQuestionnaireResponse> getResponses(@PathVariable Long caseId) {
        return questionnaireRepository.getResponsesForCase(caseId);
    }

    @PostMapping("/case/{caseId}")
    public void saveResponses(@PathVariable Long caseId, @RequestBody List<CaseQuestionnaireResponse> responses) {
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

    @PostMapping("/template/section")
    public void saveSection(@RequestBody QuestionnaireSection section) {
        questionnaireRepository.saveSection(section);
    }

    @DeleteMapping("/template/section/{id}")
    public void deleteSection(@PathVariable Long id) {
        questionnaireRepository.deleteSection(id);
    }

    @PostMapping("/template/question")
    public void saveQuestion(@RequestBody QuestionnaireQuestion question) {
        questionnaireRepository.saveQuestion(question);
    }

    @DeleteMapping("/template/question/{id}")
    public void deleteQuestion(@PathVariable Long id) {
        questionnaireRepository.deleteQuestion(id);
    }
}
