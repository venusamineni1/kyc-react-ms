package com.venus.kyc.viewer.admin;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CmmnXmlGeneratorService {

    public String generate(WorkflowConfig config) {
        List<WorkflowConfig.Stage> stages = config.getStages();
        List<WorkflowConfig.DiscretionaryAction> actions = config.getDiscretionaryActions()
                .stream().filter(WorkflowConfig.DiscretionaryAction::isEnabled).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<definitions xmlns=\"http://www.omg.org/spec/CMMN/20151109/MODEL\"\n");
        sb.append("             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("             xmlns:flowable=\"http://flowable.org/cmmn\"\n");
        sb.append("             xmlns:cmmndi=\"http://www.omg.org/spec/CMMN/20151109/CMMNDI\"\n");
        sb.append("             xmlns:dc=\"http://www.omg.org/spec/CMMN/20151109/DC\"\n");
        sb.append("             xmlns:di=\"http://www.omg.org/spec/CMMN/20151109/DI\"\n");
        sb.append("             targetNamespace=\"http://www.venus.com/kyc\">\n\n");

        sb.append("    <case id=\"").append(xml(config.getCaseKey())).append("\" name=\"")
                .append(xml(config.getCaseName())).append("\">\n");
        sb.append("        <casePlanModel id=\"").append(xml(config.getCaseKey()))
                .append("PlanModel\" name=\"KYC Case\">\n\n");

        // Sequential plan items
        sb.append("            <!-- Standard Plan Items -->\n");
        for (int i = 0; i < stages.size(); i++) {
            WorkflowConfig.Stage stage = stages.get(i);
            String piId = "pi_" + stage.getTaskDefinitionKey();
            if (i == 0) {
                sb.append("            <planItem id=\"").append(piId)
                        .append("\" name=\"").append(xml(stage.getName()))
                        .append("\" definitionRef=\"").append(stage.getTaskDefinitionKey()).append("\"/>\n");
            } else {
                WorkflowConfig.Stage prev = stages.get(i - 1);
                String sentrRef = "sentry_" + prev.getTaskDefinitionKey() + "Complete";
                sb.append("            <planItem id=\"").append(piId)
                        .append("\" name=\"").append(xml(stage.getName()))
                        .append("\" definitionRef=\"").append(stage.getTaskDefinitionKey()).append("\">\n");
                sb.append("                <entryCriterion id=\"ec_").append(stage.getTaskDefinitionKey())
                        .append("\" sentryRef=\"").append(sentrRef).append("\"/>\n");
                sb.append("            </planItem>\n");
            }
        }

        // Discretionary plan items
        if (!actions.isEmpty()) {
            sb.append("\n            <!-- Discretionary Plan Items -->\n");
            for (WorkflowConfig.DiscretionaryAction action : actions) {
                String evtPiId = "pi_" + action.getEventListenerKey();
                String taskPiId = "pi_" + action.getTaskKey();
                String sentryRef = "sentry_" + action.getEventListenerKey();
                sb.append("            <planItem id=\"").append(evtPiId)
                        .append("\" definitionRef=\"").append(action.getEventListenerKey()).append("\" />\n");
                sb.append("            <planItem id=\"").append(taskPiId)
                        .append("\" name=\"").append(xml(action.getName()))
                        .append("\" definitionRef=\"").append(action.getTaskKey()).append("\">\n");
                sb.append("                <entryCriterion id=\"ec_").append(action.getTaskKey())
                        .append("\" sentryRef=\"").append(sentryRef).append("\" />\n");
                sb.append("            </planItem>\n");
            }
        }

        // Sequential sentries (stages 0 to N-2 each need a "complete" sentry)
        sb.append("\n            <!-- Sentries -->\n");
        for (int i = 0; i < stages.size() - 1; i++) {
            WorkflowConfig.Stage stage = stages.get(i);
            String sentryId = "sentry_" + stage.getTaskDefinitionKey() + "Complete";
            String piId = "pi_" + stage.getTaskDefinitionKey();
            sb.append("            <sentry id=\"").append(sentryId).append("\">\n");
            sb.append("                <planItemOnPart id=\"onPart_").append(stage.getTaskDefinitionKey())
                    .append("Complete\" sourceRef=\"").append(piId).append("\">\n");
            sb.append("                    <standardEvent>complete</standardEvent>\n");
            sb.append("                </planItemOnPart>\n");
            sb.append("            </sentry>\n");
        }

        // Discretionary sentries
        for (WorkflowConfig.DiscretionaryAction action : actions) {
            String sentryId = "sentry_" + action.getEventListenerKey();
            String evtPiId = "pi_" + action.getEventListenerKey();
            sb.append("            <sentry id=\"").append(sentryId).append("\">\n");
            sb.append("                <planItemOnPart id=\"onPart_").append(action.getEventListenerKey())
                    .append("\" sourceRef=\"").append(evtPiId).append("\">\n");
            sb.append("                    <standardEvent>occur</standardEvent>\n");
            sb.append("                </planItemOnPart>\n");
            sb.append("            </sentry>\n");
        }

        // Sequential stage definitions
        sb.append("\n            <!-- Definitions -->\n");
        for (WorkflowConfig.Stage stage : stages) {
            sb.append("            <humanTask id=\"").append(stage.getTaskDefinitionKey())
                    .append("\" name=\"").append(xml(stage.getName()))
                    .append("\" flowable:candidateGroups=\"").append(xml(stage.getCandidateGroup()))
                    .append("\" />\n");
        }

        // Discretionary definitions
        if (!actions.isEmpty()) {
            sb.append("\n            <!-- Discretionary Definitions -->\n");
            for (WorkflowConfig.DiscretionaryAction action : actions) {
                String evtName = action.getEventListenerName() != null ? action.getEventListenerName() : action.getEventListenerKey();
                sb.append("            <userEventListener id=\"").append(action.getEventListenerKey())
                        .append("\" name=\"").append(xml(evtName)).append("\" />\n");
                sb.append("            <humanTask id=\"").append(action.getTaskKey())
                        .append("\" name=\"").append(xml(action.getName())).append("\"");
                appendTaskAttributes(sb, action);
                sb.append(" />\n");
            }
        }

        sb.append("\n        </casePlanModel>\n");
        sb.append("    </case>\n");
        sb.append("</definitions>\n");

        return sb.toString();
    }

    private void appendTaskAttributes(StringBuilder sb, WorkflowConfig.DiscretionaryAction action) {
        String key = action.getTaskKey();
        // Client communication task uses initiator as assignee (no candidateGroups)
        if ("ht_clientComm".equals(key)) {
            sb.append(" flowable:assignee=\"${initiator}\"");
        } else {
            // Other discretionary tasks use taskAssignee variable + candidateGroups
            sb.append(" flowable:assignee=\"${taskAssignee}\"");
            if (action.getCandidateGroup() != null && !action.getCandidateGroup().isBlank()) {
                sb.append(" flowable:candidateGroups=\"").append(xml(action.getCandidateGroup())).append("\"");
            }
        }
    }

    private String xml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
