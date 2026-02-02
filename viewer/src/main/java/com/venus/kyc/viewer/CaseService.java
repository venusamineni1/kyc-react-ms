package com.venus.kyc.viewer;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final CaseRepository caseRepository;
    private final QuestionnaireRepository questionnaireRepository;

    public CaseService(RuntimeService runtimeService, TaskService taskService, CaseRepository caseRepository,
            QuestionnaireRepository questionnaireRepository) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.caseRepository = caseRepository;
        this.questionnaireRepository = questionnaireRepository;
    }

    private void validateMandatoryQuestions(Long caseId) {
        List<QuestionnaireSection> template = questionnaireRepository.getTemplate();
        List<CaseQuestionnaireResponse> responses = questionnaireRepository.getResponsesForCase(caseId);

        // Map of QuestionID -> Answer
        Map<Long, String> responseMap = new HashMap<>();
        for (CaseQuestionnaireResponse r : responses) {
            responseMap.put(r.questionID(), r.answerText());
        }

        for (QuestionnaireSection section : template) {
            for (QuestionnaireQuestion q : section.questions()) {
                if (q.isMandatory()) {
                    String answer = responseMap.get(q.questionID());
                    if (answer == null || answer.trim().isEmpty()) {
                        throw new IllegalStateException(
                                "Questionnaire incomplete: Answer required for '" + q.questionText() + "'");
                    }
                }
            }
        }
    }

    @Transactional
    public Long createCase(Long clientID, String reason, String userId) {
        // Start Flowable Process
        Map<String, Object> variables = new HashMap<>();
        variables.put("clientID", clientID);
        variables.put("initiator", userId);

        var processInstance = runtimeService.startProcessInstanceByKey("kycProcess", variables);

        // Create local Case record linked to process
        // Note: Ideally we update CaseRepository to store processInstanceId
        Long caseId = caseRepository.create(clientID, reason, "KYC_ANALYST", null);

        // Update process with database ID for reference
        runtimeService.setVariable(processInstance.getId(), "caseId", caseId);

        return caseId;
    }

    public List<Map<String, Object>> getUserTasks(String userId, List<String> groups) {
        List<Task> tasks = taskService.createTaskQuery()
                .or()
                .taskAssignee(userId)
                .taskCandidateGroupIn(groups)
                .endOr()
                .includeProcessVariables()
                .orderByTaskCreateTime().desc()
                .list();

        return tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("name", task.getName());
            map.put("createTime", task.getCreateTime());
            map.put("processInstanceId", task.getProcessInstanceId());
            map.put("caseId", task.getProcessVariables().get("caseId"));
            map.put("clientID", task.getProcessVariables().get("clientID"));
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void assignTask(Long caseId, String assignee, String initiator) {
        // Find Process Instance IDs where variable "caseId" == caseId
        // Since we can't easily query tasks by process variable value directly in
        // standard API without extended query,
        // we'll query Actice Process Instances first.
        // Optimization: Use Native Query or Loop over tasks?
        // Better: Loop over active tasks if volume is low, or assume 1:1.
        // Let's use RuntimeService to find execution.

        var executions = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("caseId", caseId)
                .list();

        if (executions.isEmpty()) {
            // Case might be completed or invalid
            throw new IllegalArgumentException("No active workflow found for Case ID " + caseId);
        }

        String processInstanceId = executions.get(0).getId();

        // Find active task
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().list();
        if (tasks.isEmpty()) {
            throw new IllegalStateException("No active tasks found for Case ID " + caseId);
        }

        // Optional: Check if initiator can assign (e.g. is in candidate group or
        // admin). Skipping for now.

        // Assume single active task for this sequential workflow
        Task task = tasks.get(0);

        if (assignee != null && !assignee.isEmpty()) {
            taskService.claim(task.getId(), assignee);
            caseRepository.updateStatus(caseId, null, assignee); // Keep status, update assignee
        } else {
            taskService.unclaim(task.getId());
            caseRepository.updateStatus(caseId, null, null); // Clear assignee
        }
    }

    @Transactional
    public void completeTask(String taskId, String userId) {
        // Get task to find associated Process Instance
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task not found");
        }

        String processInstanceId = task.getProcessInstanceId();
        // Check if variable exists, otherwise handle gracefully or fetch from History
        // if needed
        Object caseIdObj = runtimeService.getVariable(processInstanceId, "caseId");
        if (caseIdObj == null) {
            // Fallback or error
            return;
        }
        Long caseId;
        if (caseIdObj instanceof Number) {
            caseId = ((Number) caseIdObj).longValue();
        } else {
            try {
                caseId = Long.parseLong(caseIdObj.toString());
            } catch (NumberFormatException e) {
                return;
            }
        }

        // VALIDATION: If Analyst Approval, check questionnaire
        if ("kycAnalystTask".equals(task.getTaskDefinitionKey())) {
            validateMandatoryQuestions(caseId);
        }

        taskService.claim(taskId, userId);
        taskService.complete(taskId);

        // Sync status with SQL table
        // We query the NEXT active task to determine the new status
        List<Task> nextTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        String nextStatus = "APPROVED"; // Default if no tasks (process ended)
        String nextAssignee = null;

        if (!nextTasks.isEmpty()) {
            Task nextTask = nextTasks.get(0);
            String taskDefKey = nextTask.getTaskDefinitionKey();
            nextStatus = switch (taskDefKey) {
                case "kycAnalystTask" -> "KYC_ANALYST";
                case "kycReviewerTask" -> "KYC_REVIEWER";
                case "afcReviewerTask" -> "AFC_REVIEWER";
                case "acoReviewerTask" -> "ACO_REVIEWER";
                default -> "UNKNOWN";
            };
        }

        caseRepository.updateStatus(caseId, nextStatus, nextAssignee);
    }

    @Transactional
    public void migrateLegacyCase(Long caseId, Long clientId, String userId) {
        // Fetch current status from DB
        var optionalCase = caseRepository.findById(caseId);
        if (optionalCase.isEmpty()) {
            return;
        }
        var kycCase = optionalCase.get();
        String currentStatus = kycCase.status();
        String assignedTo = kycCase.assignedTo();

        // Cleanup existing processes to ensure clean state for demo cases
        List<org.flowable.engine.runtime.ProcessInstance> existing = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("caseId", caseId).list();
        for (var p : existing) {
            runtimeService.deleteProcessInstance(p.getId(), "Re-migration for state sync");
        }

        // Determine start activity based on status
        String activityId = switch (currentStatus) {
            case "KYC_REVIEWER" -> "kycReviewerTask";
            case "AFC_REVIEWER" -> "afcReviewerTask";
            case "ACO_REVIEWER" -> "acoReviewerTask";
            // If KYC_ANALYST or other, default to start (null)
            default -> null;
        };

        var builder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("kycProcess")
                .variable("clientID", clientId)
                .variable("initiator", userId)
                .variable("caseId", caseId);

        org.flowable.engine.runtime.ProcessInstance processInstance = builder.start();

        if (activityId != null) {
            // Move token from default start (Analyst) to target activity
            // Use "kycAnalystTask" as the source activity ID as defined in BPMN
            try {
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(processInstance.getId())
                        .moveActivityIdTo("kycAnalystTask", activityId)
                        .changeState();
            } catch (Exception e) {
                // Log and ignore to allow process to continue at Analyst
                System.err.println("Failed to move state for case " + caseId + ": " + e.getMessage());
            }
        }

        runtimeService.setVariable(processInstance.getId(), "caseId", caseId);

        // Sync assignment
        if (assignedTo != null && !assignedTo.isEmpty()) {
            // Find the active task we just started
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
            if (!tasks.isEmpty()) {
                taskService.claim(tasks.get(0).getId(), assignedTo);
            }
        }
    }

    @Transactional
    public void deleteAllTasks() {
        List<org.flowable.engine.runtime.ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .list();
        for (org.flowable.engine.runtime.ProcessInstance instance : instances) {
            runtimeService.deleteProcessInstance(instance.getId(), "Bulk delete requested by user");
        }
    }

    public List<Map<String, Object>> getAllTasks() {
        List<Task> tasks = taskService.createTaskQuery()
                .includeProcessVariables()
                .orderByTaskCreateTime().desc()
                .list();

        return tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("name", task.getName());
            map.put("assignee", task.getAssignee());
            map.put("createTime", task.getCreateTime());
            map.put("processInstanceId", task.getProcessInstanceId());
            map.put("caseId", task.getProcessVariables().get("caseId"));
            map.put("clientID", task.getProcessVariables().get("clientID"));
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllProcessInstances() {
        List<org.flowable.engine.runtime.ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .includeProcessVariables()
                .orderByProcessInstanceId().desc()
                .list();

        return instances.stream().map(instance -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", instance.getId());
            map.put("definitionKey", instance.getProcessDefinitionKey());
            map.put("startTime", instance.getStartTime());
            map.put("caseId", instance.getProcessVariables().get("caseId"));
            map.put("clientID", instance.getProcessVariables().get("clientID"));
            map.put("initiator", instance.getProcessVariables().get("initiator"));
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void terminateProcessInstance(String processInstanceId) {
        runtimeService.deleteProcessInstance(processInstanceId, "Terminated by Admin");
    }

}
