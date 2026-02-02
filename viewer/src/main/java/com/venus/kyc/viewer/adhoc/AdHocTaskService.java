package com.venus.kyc.viewer.adhoc;

import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdHocTaskService {

    private final TaskService taskService;
    private final org.flowable.engine.IdentityService identityService;

    public AdHocTaskService(TaskService taskService, org.flowable.engine.IdentityService identityService) {
        this.taskService = taskService;
        this.identityService = identityService;
    }

    @Transactional
    public String createTask(String requestor, String assignee, String requestText, Long clientID) {
        Task task = taskService.newTask();
        task.setName("Ad-Hoc Request");
        task.setOwner(requestor);
        task.setAssignee(assignee);
        taskService.saveTask(task);

        Map<String, Object> variables = new HashMap<>();
        variables.put("status", "OPEN");
        variables.put("requestText", requestText);
        variables.put("requestor", requestor);
        variables.put("clientID", clientID); // Optional

        taskService.setVariablesLocal(task.getId(), variables);

        return task.getId();
    }

    @Transactional
    public void respondTask(String taskId, String responder, String responseText) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
            throw new IllegalArgumentException("Task not found");

        // Ensure only assignee can respond
        if (!responder.equals(task.getAssignee())) {
            throw new IllegalStateException("Only the assignee can respond to this task");
        }

        // Add comment properly attributed
        if (identityService != null) {
            try {
                identityService.setAuthenticatedUserId(responder);
                taskService.addComment(taskId, null, responseText);
            } finally {
                identityService.setAuthenticatedUserId(null);
            }
        } else {
            taskService.addComment(taskId, null, responseText);
        }

        taskService.setVariableLocal(taskId, "status", "RESPONDED");
        taskService.setVariableLocal(taskId, "responseText", responseText);
        taskService.setVariableLocal(taskId, "responder", responder);

        // Re-assign to Owner (Requestor)
        taskService.setAssignee(taskId, task.getOwner());
    }

    @Transactional
    public void reassignTask(String taskId, String requestor, String newAssignee) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
            throw new IllegalArgumentException("Task not found");

        if (!requestor.equals(task.getOwner())) {
            throw new IllegalStateException("Only the owner can reassign this task");
        }

        taskService.setVariableLocal(taskId, "status", "OPEN");
        task.setAssignee(newAssignee);
        taskService.saveTask(task);
    }

    @Transactional
    public void completeTask(String taskId, String user) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
            throw new IllegalArgumentException("Task not found");

        // Only owner can complete (or maybe admin, keeping simple for now)
        if (!user.equals(task.getOwner())) {
            throw new IllegalStateException("Only the owner can complete this task");
        }

        taskService.complete(taskId);
    }

    public List<Map<String, Object>> getMyTasks(String username) {
        // Find tasks where user is Owner OR Assignee
        List<Task> tasks = taskService.createTaskQuery()
                .or()
                .taskOwner(username)
                .taskAssignee(username)
                .endOr()
                .orderByTaskCreateTime().desc()
                .list();

        return tasks.stream().filter(t -> t.getProcessInstanceId() == null) // Only standalone tasks
                .map(t -> {
                    Map<String, Object> vars = taskService.getVariablesLocal(t.getId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("name", t.getName());
                    map.put("owner", t.getOwner());
                    map.put("assignee", t.getAssignee());
                    map.put("createTime", t.getCreateTime());
                    map.put("status", vars.get("status"));
                    map.put("requestText", vars.get("requestText"));
                    map.put("responseText", vars.get("responseText"));
                    map.put("responder", vars.get("responder"));
                    map.put("clientID", vars.get("clientID"));
                    return map;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> getTaskDetails(String taskId) {
        Task t = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (t == null)
            return null;

        Map<String, Object> vars = taskService.getVariablesLocal(t.getId());
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("name", t.getName());
        map.put("owner", t.getOwner());
        map.put("assignee", t.getAssignee());
        map.put("createTime", t.getCreateTime());
        map.put("status", vars.get("status"));
        map.put("requestText", vars.get("requestText"));
        map.put("responseText", vars.get("responseText"));
        map.put("responder", vars.get("responder"));
        map.put("clientID", vars.get("clientID"));

        // Fetch comments
        List<Map<String, Object>> comments = taskService.getTaskComments(taskId).stream()
                .map(c -> {
                    Map<String, Object> cm = new HashMap<>();
                    cm.put("message", c.getFullMessage());
                    cm.put("author", c.getUserId());
                    cm.put("time", c.getTime());
                    return cm;
                }).collect(Collectors.toList());
        map.put("comments", comments);

        return map;
    }
}
