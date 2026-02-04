package com.venus.kyc.viewer;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaseServiceTest {

    @Mock
    private CmmnHistoryService cmmnHistoryService;
    @Mock
    private CmmnRuntimeService cmmnRuntimeService;

    @Mock
    private org.flowable.engine.RuntimeService runtimeService;
    @Mock
    private org.flowable.engine.TaskService taskService;
    @Mock
    private org.flowable.cmmn.api.CmmnTaskService cmmnTaskService;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private QuestionnaireRepository questionnaireRepository;

    private CaseService caseService;

    @BeforeEach
    void setUp() {
        caseService = new CaseService(runtimeService, taskService, cmmnRuntimeService, cmmnTaskService,
                cmmnHistoryService, caseRepository, questionnaireRepository);
    }

    @Test
    void getCaseTimeline_shouldHandleNullNames() {
        // Arrange
        String caseInstanceId = "123";

        // Mock History Query
        HistoricPlanItemInstanceQuery histQuery = mock(HistoricPlanItemInstanceQuery.class);
        when(cmmnHistoryService.createHistoricPlanItemInstanceQuery()).thenReturn(histQuery);

        HistoricPlanItemInstance item1 = mock(HistoricPlanItemInstance.class);
        when(item1.getCaseInstanceId()).thenReturn(caseInstanceId);
        when(item1.getName()).thenReturn(null); // Null Name!
        when(item1.getPlanItemDefinitionId()).thenReturn("stageAnalyst");
        when(item1.getState()).thenReturn("completed");

        when(histQuery.list()).thenReturn(List.of(item1));

        // Mock Runtime Query
        PlanItemInstanceQuery runtimeQuery = mock(PlanItemInstanceQuery.class);
        when(cmmnRuntimeService.createPlanItemInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.caseInstanceId(anyString())).thenReturn(runtimeQuery);
        when(runtimeQuery.list()).thenReturn(Collections.emptyList());

        // Act
        List<CaseService.TimelineItem> timeline = caseService.getCaseTimeline(caseInstanceId);

        // Assert
        assertEquals(1, timeline.size());
        assertEquals("Analyst Review Stage", timeline.get(0).name()); // Check Fallback
    }
}
