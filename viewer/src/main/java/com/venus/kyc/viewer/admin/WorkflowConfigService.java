package com.venus.kyc.viewer.admin;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.UserEventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowConfigService {

    private static final String CASE_KEY = "kycCase";

    private final CmmnRepositoryService cmmnRepositoryService;
    private final CmmnXmlGeneratorService xmlGenerator;

    public WorkflowConfigService(CmmnRepositoryService cmmnRepositoryService,
                                 CmmnXmlGeneratorService xmlGenerator) {
        this.cmmnRepositoryService = cmmnRepositoryService;
        this.xmlGenerator = xmlGenerator;
    }

    public WorkflowConfig getConfig() {
        CaseDefinition def = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey(CASE_KEY)
                .latestVersion()
                .singleResult();

        if (def == null) {
            return buildDefaultConfig();
        }

        CmmnModel model = cmmnRepositoryService.getCmmnModel(def.getId());
        Case kycCase = model.getPrimaryCase();
        Stage planModel = kycCase.getPlanModel();

        Map<String, Sentry> sentryMap = planModel.getSentries().stream()
                .collect(Collectors.toMap(Sentry::getId, s -> s));

        List<PlanItem> allPlanItems = planModel.getPlanItems();

        List<PlanItem> sequentialPlanItems = new ArrayList<>();
        List<PlanItem> discretionaryTaskPlanItems = new ArrayList<>();
        List<PlanItem> eventListenerPlanItems = new ArrayList<>();

        for (PlanItem planItem : allPlanItems) {
            PlanItemDefinition piDef = planItem.getPlanItemDefinition();
            if (piDef instanceof UserEventListener) {
                eventListenerPlanItems.add(planItem);
            } else if (piDef instanceof HumanTask) {
                if (isDiscretionaryTask(planItem, sentryMap)) {
                    discretionaryTaskPlanItems.add(planItem);
                } else {
                    sequentialPlanItems.add(planItem);
                }
            }
        }

        List<PlanItem> orderedStages = orderSequentialStages(sequentialPlanItems, sentryMap);

        List<WorkflowConfig.Stage> stages = new ArrayList<>();
        for (int i = 0; i < orderedStages.size(); i++) {
            PlanItem pi = orderedStages.get(i);
            HumanTask ht = (HumanTask) pi.getPlanItemDefinition();
            WorkflowConfig.Stage stage = new WorkflowConfig.Stage();
            stage.setTaskDefinitionKey(ht.getId());
            stage.setName(ht.getName() != null ? ht.getName() : pi.getName());
            List<String> groups = ht.getCandidateGroups();
            stage.setCandidateGroup(groups.isEmpty() ? "" : groups.get(0));
            stage.setOrder(i);
            stages.add(stage);
        }

        List<WorkflowConfig.DiscretionaryAction> actions = new ArrayList<>();
        for (PlanItem evtPi : eventListenerPlanItems) {
            UserEventListener uel = (UserEventListener) evtPi.getPlanItemDefinition();
            PlanItem taskPi = findAssociatedTask(evtPi, discretionaryTaskPlanItems, sentryMap);
            if (taskPi != null) {
                HumanTask ht = (HumanTask) taskPi.getPlanItemDefinition();
                WorkflowConfig.DiscretionaryAction action = new WorkflowConfig.DiscretionaryAction();
                action.setEventListenerKey(uel.getId());
                action.setEventListenerName(uel.getName());
                action.setTaskKey(ht.getId());
                action.setName(ht.getName() != null ? ht.getName() : taskPi.getName());
                action.setEnabled(true);
                List<String> groups = ht.getCandidateGroups();
                action.setCandidateGroup(groups.isEmpty() ? "" : String.join(",", groups));
                actions.add(action);
            }
        }

        WorkflowConfig config = new WorkflowConfig();
        config.setCaseKey(kycCase.getId());
        config.setCaseName(kycCase.getName());
        config.setVersion(def.getVersion());
        config.setDeploymentId(def.getDeploymentId());
        config.setStages(stages);
        config.setDiscretionaryActions(actions);
        return config;
    }

    public void deploy(WorkflowConfig config) {
        if (config.getStages() == null || config.getStages().isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one stage");
        }
        String xml = xmlGenerator.generate(config);
        cmmnRepositoryService.createDeployment()
                .name("KYC Workflow - Admin Update")
                .addString(CASE_KEY + ".cmmn", xml)
                .deploy();
    }

    // --- Parsing helpers ---

    private boolean isDiscretionaryTask(PlanItem planItem, Map<String, Sentry> sentryMap) {
        for (Criterion criterion : planItem.getEntryCriteria()) {
            Sentry sentry = sentryMap.get(criterion.getSentryRef());
            if (sentry != null) {
                for (SentryOnPart onPart : sentry.getOnParts()) {
                    if ("occur".equals(onPart.getStandardEvent())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<PlanItem> orderSequentialStages(List<PlanItem> stages, Map<String, Sentry> sentryMap) {
        if (stages.isEmpty()) return stages;

        // Find first stage (no entry criteria)
        PlanItem first = stages.stream()
                .filter(pi -> pi.getEntryCriteria().isEmpty())
                .findFirst()
                .orElse(stages.get(0));

        // Build map: sourceRef plan item ID -> dependent plan item
        Map<String, PlanItem> dependencyMap = new HashMap<>();
        for (PlanItem pi : stages) {
            for (Criterion criterion : pi.getEntryCriteria()) {
                Sentry sentry = sentryMap.get(criterion.getSentryRef());
                if (sentry != null) {
                    for (SentryOnPart onPart : sentry.getOnParts()) {
                        if ("complete".equals(onPart.getStandardEvent())) {
                            dependencyMap.put(onPart.getSourceRef(), pi);
                        }
                    }
                }
            }
        }

        // Follow the chain from first stage
        List<PlanItem> ordered = new ArrayList<>();
        PlanItem current = first;
        Set<String> visited = new HashSet<>();
        while (current != null && visited.add(current.getId())) {
            ordered.add(current);
            current = dependencyMap.get(current.getId());
        }
        return ordered;
    }

    private PlanItem findAssociatedTask(PlanItem evtPlanItem, List<PlanItem> tasks,
                                        Map<String, Sentry> sentryMap) {
        for (PlanItem taskPi : tasks) {
            for (Criterion criterion : taskPi.getEntryCriteria()) {
                Sentry sentry = sentryMap.get(criterion.getSentryRef());
                if (sentry != null) {
                    for (SentryOnPart onPart : sentry.getOnParts()) {
                        if (evtPlanItem.getId().equals(onPart.getSourceRef())) {
                            return taskPi;
                        }
                    }
                }
            }
        }
        return null;
    }

    private WorkflowConfig buildDefaultConfig() {
        WorkflowConfig config = new WorkflowConfig();
        config.setCaseKey(CASE_KEY);
        config.setCaseName("KYC Case Management (CMMN)");
        config.setVersion(0);
        config.setDeploymentId(null);

        List<WorkflowConfig.Stage> stages = List.of(
                stage("ht_analystReview", "Analyst Review", "ROLE_KYC_ANALYST", 0),
                stage("ht_reviewerReview", "Reviewer Review", "ROLE_KYC_REVIEWER", 1),
                stage("ht_afcStandardReview", "AFC Review", "ROLE_AFC_REVIEWER", 2),
                stage("ht_acoReview", "ACO Final Review", "ROLE_ACO_REVIEWER", 3)
        );
        config.setStages(stages);

        List<WorkflowConfig.DiscretionaryAction> actions = List.of(
                action("evtInitiateCommunication", "Initiate Client Communication",
                        "ht_clientComm", "Request Additional Documentation", true, ""),
                action("evtChallengeScreening", "Challenge Screening Hit",
                        "ht_afcDiscretionaryReview", "AFC Screening Approval", true, "ROLE_AFC_REVIEWER"),
                action("evtOverrideRisk", "Override Risk Assessment",
                        "ht_riskOverride", "Risk Override Peer Approval", true, "ROLE_KYC_REVIEWER,ROLE_KYC_ANALYST")
        );
        config.setDiscretionaryActions(actions);
        return config;
    }

    private WorkflowConfig.Stage stage(String key, String name, String group, int order) {
        WorkflowConfig.Stage s = new WorkflowConfig.Stage();
        s.setTaskDefinitionKey(key);
        s.setName(name);
        s.setCandidateGroup(group);
        s.setOrder(order);
        return s;
    }

    private WorkflowConfig.DiscretionaryAction action(String evtKey, String evtName,
                                                       String taskKey, String name,
                                                       boolean enabled, String group) {
        WorkflowConfig.DiscretionaryAction a = new WorkflowConfig.DiscretionaryAction();
        a.setEventListenerKey(evtKey);
        a.setEventListenerName(evtName);
        a.setTaskKey(taskKey);
        a.setName(name);
        a.setEnabled(enabled);
        a.setCandidateGroup(group);
        return a;
    }
}
