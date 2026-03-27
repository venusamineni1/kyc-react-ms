package com.venus.kyc.viewer;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CaseService {

    private final CmmnRuntimeService cmmnRuntimeService;
    private final CmmnTaskService cmmnTaskService;
    private final CmmnHistoryService cmmnHistoryService;
    private final CaseRepository caseRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final ClientRepository clientRepository;
    private final com.venus.kyc.viewer.risk.RiskAssessmentService riskService;
    private final com.venus.kyc.viewer.screening.ScreeningService screeningService;

    public CaseService(CmmnRuntimeService cmmnRuntimeService, CmmnTaskService cmmnTaskService,
            CmmnHistoryService cmmnHistoryService,
            CaseRepository caseRepository, QuestionnaireRepository questionnaireRepository,
            ClientRepository clientRepository,
            com.venus.kyc.viewer.risk.RiskAssessmentService riskService,
            com.venus.kyc.viewer.screening.ScreeningService screeningService) {
        this.cmmnRuntimeService = cmmnRuntimeService;
        this.cmmnTaskService = cmmnTaskService;
        this.cmmnHistoryService = cmmnHistoryService;
        this.caseRepository = caseRepository;
        this.questionnaireRepository = questionnaireRepository;
        this.clientRepository = clientRepository;
        this.riskService = riskService;
        this.screeningService = screeningService;
    }

    public record TimelineItem(
            String name,
            String status,
            Date startTime,
            Date endTime,
            String itemType) {
    }

    @Transactional
    public Long createCase(Long clientID, String reason, String userId) {
        // Start Flowable CMMN Case
        Map<String, Object> variables = new HashMap<>();
        variables.put("clientID", clientID);
        variables.put("initiator", userId);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("kycCase")
                .variables(variables)
                .start();

        // Create local Case record linked to case instance
        Long caseId = caseRepository.create(clientID, reason, "KYC_ANALYST", null, caseInstance.getId(), "CMMN");

        // Update case instance with database ID for reference
        cmmnRuntimeService.setVariable(caseInstance.getId(), "caseId", caseId);

        return caseId;
    }

    public List<Map<String, Object>> getUserTasks(String userId, List<String> groups) {
        // Expand groups to include ROLE_ prefix if missing, to match CMMN definitions
        List<String> expandedGroups = new ArrayList<>(groups);
        for (String g : groups) {
            if (!g.startsWith("ROLE_")) {
                expandedGroups.add("ROLE_" + g);
            }
        }

        // CMMN Tasks Only
        List<Task> cmmnTasks = cmmnTaskService.createTaskQuery()
                .or()
                .taskAssignee(userId)
                .taskCandidateGroupIn(expandedGroups)
                .endOr()
                .includeCaseVariables()
                .orderByTaskCreateTime().desc()
                .list();

        return cmmnTasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("name", task.getName());
            map.put("createTime", task.getCreateTime());
            map.put("caseInstanceId", task.getScopeId()); // CMMN Case Instance ID
            map.put("workflowType", "CMMN");

            // Handle variables
            Map<String, Object> vars = task.getCaseVariables();

            if (vars != null) {
                map.put("caseId", vars.get("caseId"));
                map.put("clientID", vars.get("clientID"));
                map.put("initiator", vars.get("initiator"));
            }
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTasksForCase(Long caseId) {
        return getTasksForCase(caseId, null, null);
    }

    public List<Map<String, Object>> getTasksForCase(Long caseId, String userId, List<String> groups) {
        var caseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                .variableValueEquals("caseId", caseId)
                .list();

        if (caseInstances.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        String caseInstanceId = caseInstances.get(0).getId();
        var query = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstanceId)
                .includeCaseVariables();

        if (userId != null && groups != null) {
            List<String> expandedGroups = new ArrayList<>(groups);
            for (String g : groups) {
                if (!g.startsWith("ROLE_")) {
                    expandedGroups.add("ROLE_" + g);
                }
            }
            query = query.or()
                    .taskAssignee(userId)
                    .taskCandidateGroupIn(expandedGroups)
                    .endOr();
        }

        List<Task> cmmnTasks = query.orderByTaskCreateTime().desc().list();

        return cmmnTasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("name", task.getName());
            map.put("createTime", task.getCreateTime());
            map.put("caseInstanceId", task.getScopeId()); // CMMN Case Instance ID
            map.put("assignee", task.getAssignee());
            map.put("workflowType", "CMMN");

            Map<String, Object> vars = task.getCaseVariables();
            if (vars != null) {
                map.put("caseId", vars.get("caseId"));
                map.put("clientID", vars.get("clientID"));
                map.put("initiator", vars.get("initiator"));
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void assignTask(Long caseId, String assignee, String initiator) {
        // ALWAYS update the database record first for consistent display in header
        caseRepository.updateStatus(caseId, null, assignee);

        // 1. Try CMMN
        var caseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                .variableValueEquals("caseId", caseId)
                .list();

        if (!caseInstances.isEmpty()) {
            String caseInstanceId = caseInstances.get(0).getId();
            List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).active().list();
            if (!tasks.isEmpty()) {
                handleAssignTask(tasks.get(0), assignee);
                return;
            }
        }

        // 2. Try BPMN (BpmnRuntimeService would be needed, but for now we look in the main task service)
        // If not assigned via CMMN, check all active tasks for this caseId (some might be BPMN)
        // Note: In this architecture, we primarily use CMMN for cases, but we check all tasks to be safe.
        // If CMMN failed or was empty, we can still return successfully as we updated the DB.
    }

    private void handleAssignTask(Task task, String assignee) {
        if (assignee != null && !assignee.isEmpty()) {
            // Use setAssignee to force assignment even if already claimed
            cmmnTaskService.setAssignee(task.getId(), assignee);
        } else {
            cmmnTaskService.unclaim(task.getId());
        }
    }

    @Transactional
    public void completeTask(String taskId, String userId) {
        completeTask(taskId, userId, null);
    }

    @Transactional
    public void completeTask(String taskId, String userId, String action) {
        // Try CMMN
        Task task = cmmnTaskService.createTaskQuery().taskId(taskId).singleResult();
        if (task != null) {
            completeCmmnTask(task, userId, action);
            return;
        }

        throw new IllegalArgumentException("Task not found: " + taskId);
    }

    private void completeCmmnTask(Task task, String userId, String action) {
        String caseInstanceId = task.getScopeId();
        Object caseIdObj = cmmnRuntimeService.getVariable(caseInstanceId, "caseId");
        if (caseIdObj == null)
            return;
        Long caseId = Long.parseLong(caseIdObj.toString());

        Object clientIdObj = cmmnRuntimeService.getVariable(caseInstanceId, "clientID");
        Long clientId = null;
        if (clientIdObj != null) {
            clientId = Long.parseLong(clientIdObj.toString());
        }

        if (task.getAssignee() == null) {
            cmmnTaskService.claim(task.getId(), userId);
        } else if (!task.getAssignee().equals(userId)) {
            // Optional: You could allow 'stealing' the task by calling setAssignee
            // but for now let's respect the existing claim to avoid conflicts.
            // cmmnTaskService.setAssignee(task.getId(), userId);
        }

        cmmnTaskService.complete(task.getId());

        // Update Status logic based on task definition
        String taskKey = task.getTaskDefinitionKey();
        String nextStatus = "PROCESSING";

        if ("ht_acoReview".equals(taskKey)) {
            if ("APPROVE".equalsIgnoreCase(action)) {
                nextStatus = "APPROVED";
            } else if ("REJECT".equalsIgnoreCase(action)) {
                nextStatus = "REJECTED";
            } else {
                nextStatus = "COMPLETED";
            }
        } else if ("ht_analystReview".equals(taskKey)) {
            nextStatus = "REVIEWER_REVIEW";
        } else if ("ht_reviewerReview".equals(taskKey)) {
            nextStatus = "AFC_REVIEW";
        } else if ("ht_afcStandardReview".equals(taskKey)) {
            nextStatus = "ACO_REVIEW";
        }

        caseRepository.updateStatus(caseId, nextStatus, null);

        if (clientId != null && ("APPROVED".equals(nextStatus) || "REJECTED".equals(nextStatus))) {
            clientRepository.updateClientStatus(clientId, nextStatus);
            sendFeedbackToClientFacingSystem(clientId, nextStatus);
        }
    }

    private void sendFeedbackToClientFacingSystem(Long clientId, String finalDecision) {
        try {
            org.slf4j.LoggerFactory.getLogger(CaseService.class).info(
                "MOCK WEBHOOK: Sending final decision to client-facing system for client {}. Decision: {}",
                clientId, finalDecision
            );
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(CaseService.class).error("Error sending feedback", e);
        }
    }

    @Transactional
    public void migrateLegacyCase(Long caseId, Long clientId, String userId) {
        Optional<Case> cOpt = caseRepository.findById(caseId);
        if (cOpt.isEmpty())
            return;
        Case c = cOpt.get();

        if (c.instanceID() != null && !c.instanceID().isEmpty()) {
            return; // Already has an instance
        }

        // Start Flowable CMMN Case
        Map<String, Object> variables = new HashMap<>();
        variables.put("clientID", clientId);
        variables.put("initiator", userId);
        variables.put("caseId", caseId); // Important for reverse lookup

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("kycCase")
                .variables(variables)
                .start();

        // Update DB
        caseRepository.updateInstanceInfo(caseId, caseInstance.getId(), "CMMN");

        // Sync Assignment if exists
        if (c.assignedTo() != null && !c.assignedTo().isEmpty()) {
            List<Task> tasks = cmmnTaskService.createTaskQuery()
                    .caseInstanceId(caseInstance.getId())
                    .active()
                    .list();
            if (!tasks.isEmpty()) {
                cmmnTaskService.claim(tasks.get(0).getId(), c.assignedTo());
            }
        }

        // Add a comment
        caseRepository.addComment(caseId, "SYSTEM", "Migrated to CMMN workflow", "SYSTEM");
    }

    @Transactional
    public void deleteAllTasks() {
        // CMMN
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().list();
        for (var instance : caseInstances) {
            cmmnRuntimeService.terminateCaseInstance(instance.getId());
        }
    }

    public List<Map<String, Object>> getAllTasks() {
        List<Task> cmmnTasks = cmmnTaskService.createTaskQuery().includeCaseVariables().orderByTaskCreateTime().desc()
                .list();

        return cmmnTasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("name", task.getName());
            map.put("assignee", task.getAssignee());
            map.put("createTime", task.getCreateTime());
            map.put("caseInstanceId", task.getScopeId());

            Map<String, Object> vars = task.getCaseVariables();

            if (vars != null) {
                map.put("caseId", vars.get("caseId"));
                map.put("clientID", vars.get("clientID"));
            }
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllCaseInstances() {
        List<CaseInstance> instances = cmmnRuntimeService.createCaseInstanceQuery()
                .includeCaseVariables()
                .orderByCaseInstanceId().desc()
                .list();

        return instances.stream().map(instance -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", instance.getId());
            map.put("definitionKey", instance.getCaseDefinitionKey());
            map.put("startTime", instance.getStartTime());
            map.put("caseId", instance.getCaseVariables().get("caseId"));
            map.put("clientID", instance.getCaseVariables().get("clientID"));
            map.put("initiator", instance.getCaseVariables().get("initiator"));
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void terminateProcessInstance(String instanceId) {
        try {
            cmmnRuntimeService.terminateCaseInstance(instanceId);
        } catch (Exception e) {
            // Ignore
        }
    }

    private String getPlanItemName(String name, String definitionId) {
        if (name != null && !name.isEmpty())
            return name;
        if (definitionId == null)
            return "Unnamed Task";

        // Logic similar to getAvailableDiscretionaryActions fallbacks
        if ("evtInitiateCommunication".equals(definitionId))
            return "Initiate Client Communication";
        if ("evtChallengeScreening".equals(definitionId))
            return "Challenge Screening Hit";
        if ("evtOverrideRisk".equals(definitionId))
            return "Override Risk Assessment";
        if ("stageAnalyst".equals(definitionId))
            return "Analyst Review Stage";
        if ("stageReviewer".equals(definitionId))
            return "Reviewer Review Stage";
        if ("stageAFC".equals(definitionId))
            return "AFC Review Stage";
        if ("stageACO".equals(definitionId))
            return "ACO Review Stage";

        return definitionId;
    }

    public List<TimelineItem> getCaseTimeline(String caseInstanceId) {
        List<TimelineItem> timeline = new ArrayList<>();

        // 1. Get Historical Plan Items (Completed/Terminated/etc.)
        List<HistoricPlanItemInstance> allHistoricItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .list();

        List<HistoricPlanItemInstance> historicItems = allHistoricItems.stream()
                .filter(item -> caseInstanceId.equals(item.getCaseInstanceId()))
                .collect(Collectors.toList());

        for (HistoricPlanItemInstance item : historicItems) {
            timeline.add(new TimelineItem(
                    getPlanItemName(item.getName(), item.getPlanItemDefinitionId()),
                    item.getState(),
                    item.getCreateTime(),
                    item.getEndedTime(),
                    item.getPlanItemDefinitionType()));
        }

        // 2. Get Runtime Plan Items (Active/Available/Enabled)
        List<PlanItemInstance> runtimeItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .list();

        for (PlanItemInstance item : runtimeItems) {
            String safeName = getPlanItemName(item.getName(), item.getPlanItemDefinitionId());
            // Only add if not already in history (Flowable might overlap depending on
            // state)
            boolean exists = timeline.stream()
                    .anyMatch(t -> Objects.equals(t.name(), safeName) && t.endTime() == null);
            if (!exists) {
                timeline.add(new TimelineItem(
                        safeName,
                        item.getState(),
                        item.getCreateTime(),
                        null,
                        item.getPlanItemDefinitionType()));
            }
        }

        // 3. Sort by start time
        timeline.sort(Comparator.comparing(TimelineItem::startTime,
                Comparator.nullsFirst(Comparator.naturalOrder())));

        return timeline;
    }

    public List<Map<String, Object>> getAvailableDiscretionaryActions(String instanceId) {
        // In Flowable CMMN, UserEventListeners can be queried directly
        List<UserEventListenerInstance> listeners = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(instanceId)
                .list();

        return listeners.stream()
                .filter(item -> "available".equals(item.getState()) || "enabled".equals(item.getState()))
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    String name = item.getName();
                    if (name == null || name.isEmpty()) {
                        if ("evtInitiateCommunication".equals(item.getPlanItemDefinitionId()))
                            name = "Initiate Client Communication";
                        else if ("evtChallengeScreening".equals(item.getPlanItemDefinitionId()))
                            name = "Challenge Screening Hit";
                        else if ("evtOverrideRisk".equals(item.getPlanItemDefinitionId()))
                            name = "Override Risk Assessment";
                        else
                            name = item.getPlanItemDefinitionId();
                    }
                    map.put("name", name);
                    map.put("definitionId", item.getPlanItemDefinitionId());
                    return map;
                }).collect(Collectors.toList());
    }

    @Transactional
    public void triggerDiscretionaryAction(String caseInstanceId, String planItemInstanceId,
            Map<String, Object> variables) {
        if (variables != null && !variables.isEmpty()) {
            cmmnRuntimeService.setVariables(caseInstanceId, variables);
        }
        // Trigger the User Event Listener
        cmmnRuntimeService.completeUserEventListenerInstance(planItemInstanceId);
    }

    public List<Map<String, Object>> getAllTasksDebug() {
        List<Task> tasks = new ArrayList<>();
        tasks.addAll(cmmnTaskService.createTaskQuery().includeCaseVariables().list());

        return tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("name", task.getName());
            map.put("assignee", task.getAssignee());
            map.put("definitionKey", task.getTaskDefinitionKey());
            map.put("scopeType", task.getScopeType());
            map.put("scopeId", task.getScopeId());

            Map<String, Object> vars = task.getCaseVariables();
            if (vars != null) {
                map.put("caseId", vars.get("caseId"));
            }
            return map;
        }).collect(Collectors.toList());
    }
}
