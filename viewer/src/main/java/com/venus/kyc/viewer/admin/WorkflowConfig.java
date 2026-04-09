package com.venus.kyc.viewer.admin;

import java.util.List;

public class WorkflowConfig {

    private String caseKey;
    private String caseName;
    private int version;
    private String deploymentId;
    private List<Stage> stages;
    private List<DiscretionaryAction> discretionaryActions;

    public String getCaseKey() { return caseKey; }
    public void setCaseKey(String caseKey) { this.caseKey = caseKey; }

    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getDeploymentId() { return deploymentId; }
    public void setDeploymentId(String deploymentId) { this.deploymentId = deploymentId; }

    public List<Stage> getStages() { return stages; }
    public void setStages(List<Stage> stages) { this.stages = stages; }

    public List<DiscretionaryAction> getDiscretionaryActions() { return discretionaryActions; }
    public void setDiscretionaryActions(List<DiscretionaryAction> discretionaryActions) { this.discretionaryActions = discretionaryActions; }

    public static class Stage {
        private String taskDefinitionKey;
        private String name;
        private String candidateGroup;
        private int order;

        public String getTaskDefinitionKey() { return taskDefinitionKey; }
        public void setTaskDefinitionKey(String taskDefinitionKey) { this.taskDefinitionKey = taskDefinitionKey; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCandidateGroup() { return candidateGroup; }
        public void setCandidateGroup(String candidateGroup) { this.candidateGroup = candidateGroup; }

        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
    }

    public static class DiscretionaryAction {
        private String eventListenerKey;
        private String eventListenerName;
        private String taskKey;
        private String name;
        private boolean enabled;
        private String candidateGroup;

        public String getEventListenerKey() { return eventListenerKey; }
        public void setEventListenerKey(String eventListenerKey) { this.eventListenerKey = eventListenerKey; }

        public String getEventListenerName() { return eventListenerName; }
        public void setEventListenerName(String eventListenerName) { this.eventListenerName = eventListenerName; }

        public String getTaskKey() { return taskKey; }
        public void setTaskKey(String taskKey) { this.taskKey = taskKey; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getCandidateGroup() { return candidateGroup; }
        public void setCandidateGroup(String candidateGroup) { this.candidateGroup = candidateGroup; }
    }
}
