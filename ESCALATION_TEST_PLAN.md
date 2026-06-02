# KYC Workflow Escalation - Test Plan & Verification

## Summary of Changes

### 1. CMMN Workflow (`kyc-case.cmmn`)
**Status:** ✅ Completed

**Changes:**
- Replaced linear sequential sentries with conditional sentries using `<ifPart>` and `<condition>` elements
- Plan items for ACO and AFC now activate based on `nextStage` variable
- Added case variables: `nextStage`, `escalationLevel`, `escalationPath`, `escalationReason`, `escalationInitiator`

**Key Sentries:**
- `sentry_escalationToReviewer`: Activates when `nextStage == 'REVIEWER'`
- `sentry_escalationToACO`: Activates when `nextStage == 'ACO'`
- `sentry_escalationToAFC`: Activates when `nextStage == 'AFC'`

---

### 2. Backend Service (`CaseService.java`)
**Status:** ✅ Completed

**New Methods:**
1. `escalateCase(Long caseId, String targetRole, String userId, String reason)`
   - Validates escalation path
   - Tracks escalation history in case variables
   - Completes current task to trigger conditional sentries
   - Adds audit comment

2. `validateEscalationPath(String fromStage, String toStage)`
   - Validates allowed escalation routes:
     - KYC_ANALYST → ACO or AFC
     - KYC_REVIEWER → ACO
     - ACO → AFC

**Updated Methods:**
- `createCase()`: Initializes escalation variables
- `completeCmmnTask()`: Handles ESCALATE_ACO and ESCALATE_AFC actions, sets `nextStage` based on task key
- `reworkCase()`: Resets escalation variables when case is reworked
- `migrateLegacyCase()`: Initializes escalation variables for migrated cases

---

### 3. API Controller (`CaseController.java`)
**Status:** ✅ Completed

**New Endpoint:**
- `POST /api/cases/{id}/escalate`
  - Request body: `{ "targetRole": "ACO|AFC", "reason": "optional" }`
  - Validates escalation, logs to comments and audit

**Updated Endpoint:**
- `POST /api/cases/{id}/transition`
  - Now supports ESCALATE_ACO and ESCALATE_AFC actions
  - Can be called from UI to trigger escalation inline with other actions

---

### 4. Frontend - WorkflowConfig Page (`WorkflowConfig.jsx`)
**Status:** ✅ Completed

**Changes:**
- Added ESCALATION_PATHS constant documenting valid escalation routes
- Added new section "Escalation Paths" displaying allowed escalations
- Shows: KYC Analyst → ACO/AFC, Reviewer → ACO, ACO → AFC
- Read-only documentation for configuration reference

---

### 5. Frontend - Workflow Diagram (`WorkflowDiagram.jsx`)
**Status:** ✅ Completed

**Changes:**
- Added escalation arrow markers and colors
- Renders curved dashed arrows showing escalation paths:
  - Analyst → ACO (orange)
  - Analyst → AFC (purple, if AFC exists)
  - Reviewer → ACO (orange, if reviewer exists)
  - ACO → AFC (purple, if both exist)
- Updated legend to include escalation routes
- Arrows are conditional based on number of stages (N)

---

## Manual Testing Workflow

### Test Case 1: Normal Flow (Analyst → Reviewer → AFC → ACO)
**Expected Result:** Case flows through all stages sequentially without escalation

**Steps:**
1. Create a case as KYC_ANALYST
2. Complete Analyst task with SUBMIT action
3. Verify Reviewer task activates
4. Complete Reviewer task with SUBMIT action
5. Verify AFC task activates
6. Complete AFC task with SUBMIT action
7. Verify ACO task activates
8. Complete ACO task with APPROVE
9. Verify case status = APPROVED

**Assertions:**
- `escalationLevel` = 0 throughout
- `escalationPath` = [] (empty)
- Status transitions: KYC_ANALYST → REVIEWER → AFC → ACO → APPROVED

---

### Test Case 2: Analyst Escalates to ACO
**Expected Result:** Case skips Reviewer and AFC, goes directly to ACO

**Steps:**
1. Create a case as KYC_ANALYST
2. Escalate using POST `/api/cases/{id}/escalate` with `targetRole=ACO`
3. Verify ACO task activates (Reviewer and AFC are skipped)
4. Complete ACO task with APPROVE
5. Verify case is APPROVED

**Assertions:**
- ACO task is created without Reviewer/AFC tasks being created
- `escalationLevel` = 1
- `escalationPath` contains one entry: `{from: "KYC_ANALYST", to: "ACO", ...}`
- Status: KYC_ANALYST → ACO → APPROVED

---

### Test Case 3: Analyst Escalates to AFC (Challenge Screening Hit)
**Expected Result:** Case skips Reviewer and ACO, goes to AFC

**Steps:**
1. Create a case
2. Escalate using POST `/api/cases/{id}/escalate` with `targetRole=AFC`, `reason=Challenge screening hit`
3. Verify AFC task activates
4. Complete AFC task with APPROVE
5. Verify case is APPROVED

**Assertions:**
- Only AFC task is active (Reviewer and ACO skipped)
- `escalationLevel` = 1
- `escalationReason` = "Challenge screening hit"
- Status: KYC_ANALYST → AFC → APPROVED

---

### Test Case 4: Reviewer Escalates to ACO
**Expected Result:** Case skips AFC, goes directly to ACO

**Steps:**
1. Create a case and flow to Reviewer task
2. Complete Reviewer task with SUBMIT (or escalate)
3. If didn't escalate in step 2, escalate with POST with `targetRole=ACO`
4. Verify ACO task activates (AFC is skipped)
5. Complete ACO with APPROVE

**Assertions:**
- ACO task activates after Reviewer
- AFC task is not created
- `escalationPath[last].from` = "KYC_REVIEWER", `to` = "ACO"

---

### Test Case 5: ACO Escalates to AFC
**Expected Result:** Case goes from ACO to AFC

**Steps:**
1. Create case and flow through Reviewer
2. Escalate to ACO from Reviewer
3. Escalate to AFC with POST `/api/cases/{id}/escalate` with `targetRole=AFC`
4. Verify AFC task activates
5. Complete AFC with APPROVE

**Assertions:**
- AFC task activates after ACO
- `escalationLevel` = 2
- `escalationPath[1].from` = "ACO", `to` = "AFC"
- Status progression: KYC_ANALYST → REVIEWER → ACO → AFC → APPROVED

---

### Test Case 6: Rework Resets Escalation
**Expected Result:** Case sent for rework clears escalation path

**Steps:**
1. Create case and escalate to ACO
2. Verify ACO task is active
3. Send back for rework with POST `/api/cases/{id}/rework` with comment
4. Verify case status = KYC_ANALYST
5. Verify new Analyst task is created
6. Complete Analyst task with SUBMIT
7. Verify Reviewer task activates (normal flow, not escalated)

**Assertions:**
- After rework, `escalationLevel` = 0
- After rework, `escalationPath` = [] (empty)
- `nextStage` is reset to "REVIEWER"
- Case flows through normal Reviewer stage (escalation path cleared)

---

### Test Case 7: Invalid Escalation Rejected
**Expected Result:** Escalation from unsupported stage is rejected

**Steps:**
1. Create case and flow to AFC stage
2. Attempt to escalate with POST `/api/cases/{id}/escalate` with `targetRole=ACO`
3. Expect 400 Bad Request response
4. Verify case remains at AFC stage

**Assertions:**
- HTTP 400 response
- Case status unchanged
- Error message: "Cannot escalate from AFC to ACO"
- No escalation event logged

---

### Test Case 8: Escalation Path Tracking
**Expected Result:** Full escalation history is maintained

**Steps:**
1. Create case
2. Escalate Analyst → ACO
3. Escalate ACO → AFC
4. Query case variables for `escalationPath`
5. Verify it contains both escalation events

**Assertions:**
- `escalationPath` is array with 2 entries
- Entry 1: `{from: "KYC_ANALYST", to: "ACO", initiator: "user1", reason: "", timestamp: "..."}`
- Entry 2: `{from: "ACO", to: "AFC", initiator: "user1", reason: "", timestamp: "..."}`

---

### Test Case 9: Approval/Rejection from Any Stage
**Expected Result:** Any stage can approve/reject to close case

**Steps:**
1. Create case and escalate to AFC
2. Complete AFC task with APPROVE
3. Verify case status = APPROVED
4. Create another case and escalate to ACO
5. Complete ACO task with REJECT
6. Verify case status = REJECTED

**Assertions:**
- Status transitions to APPROVED or REJECTED regardless of current stage
- Case is marked as closed
- Client status is updated (if applicable)

---

### Test Case 10: Cancel Case (Analyst Only)
**Expected Result:** Analyst can cancel case at any time

**Steps:**
1. Create case
2. Cancel with POST `/api/cases/{id}/cancel`
3. Verify case status = CANCELLED
4. Verify workflow instance is terminated

**Assertions:**
- Case status = CANCELLED
- No further tasks can be created
- Audit log shows "CANCEL_CASE" event

---

## UI/Integration Tests

### Workflow Diagram Visualization
**Verify:**
1. Open WorkflowConfig page
2. Check "Escalation Paths" section displays all 4 valid routes
3. Verify arrows are rendered in diagram:
   - Analyst → ACO (orange)
   - Analyst → AFC (purple)
   - Reviewer → ACO (orange)
   - ACO → AFC (purple)
4. Legend includes "Escalate (manual routing)" entry

---

### Case Details UI (if applicable)
**Verify:**
1. Open case details page
2. Check escalation path is displayed (if escalated)
3. Timeline shows escalation events
4. Available actions include escalation options (for authorized users)

---

## Automated Test Cases (Unit & Integration)

### Unit Tests (CaseService)
```java
@Test
void testValidateEscalationPath_AnalystToACO() { ... }

@Test
void testValidateEscalationPath_AnalystToAFC() { ... }

@Test
void testValidateEscalationPath_ReviewerToACO() { ... }

@Test
void testValidateEscalationPath_ACOToAFC() { ... }

@Test
void testValidateEscalationPath_InvalidPath_AFC_to_Reviewer() { ... }

@Test
void testEscalateCase_TracksPathAndLevel() { ... }

@Test
void testReworkCase_ResetsEscalation() { ... }
```

### Integration Tests (End-to-End CMMN)
```java
@Test
void testEscalationFlow_AnalystToACO() { 
    // Create case → escalate to ACO → verify ACO task activates
}

@Test
void testEscalationFlow_AnalystToAFC() {
    // Create case → escalate to AFC → verify AFC task activates
}

@Test
void testEscalationFlow_MultipleEscalations() {
    // Create case → escalate to ACO → escalate to AFC → verify path
}

@Test
void testReworkResetsEscalation() {
    // Create → escalate → rework → verify normal flow resumes
}
```

---

## API Integration Tests

### POST /api/cases/{id}/escalate
```bash
# Valid escalation
curl -X POST http://localhost:8080/api/cases/1/escalate \
  -H "Content-Type: application/json" \
  -d '{"targetRole": "ACO", "reason": "Regulatory check"}'

# Expected: 200 OK

# Invalid escalation
curl -X POST http://localhost:8080/api/cases/1/escalate \
  -H "Content-Type: application/json" \
  -d '{"targetRole": "UNKNOWN"}'

# Expected: 400 Bad Request
```

### POST /api/cases/{id}/transition with escalation
```bash
curl -X POST http://localhost:8080/api/cases/1/transition \
  -H "Content-Type: application/json" \
  -d '{"action": "ESCALATE_ACO", "comment": "Escalating for review", "reason": "Risk assessment"}'

# Expected: 200 OK
```

---

## Data Verification Queries

### Check escalation variables in CMMN
```java
// After escalating case to ACO
Object nextStage = cmmnRuntimeService.getVariable(caseInstanceId, "nextStage");
assertEquals("ACO", nextStage);

Object level = cmmnRuntimeService.getVariable(caseInstanceId, "escalationLevel");
assertEquals(1, level);

Object path = cmmnRuntimeService.getVariable(caseInstanceId, "escalationPath");
assertNotNull(path);
assertTrue(path instanceof List);
```

### Check case comments for escalation events
```java
List<CaseComment> comments = caseRepository.findCommentsByCaseId(caseId);
Optional<CaseComment> escalationComment = comments.stream()
    .filter(c -> c.text().startsWith("ESCALATE:"))
    .findFirst();
assertTrue(escalationComment.isPresent());
```

---

## Regression Testing

### Ensure existing workflows still work
- [ ] Standard 4-stage flow (Analyst → Reviewer → AFC → ACO) completes
- [ ] Rework from Reviewer, ACO, AFC flows back to Analyst properly
- [ ] Cancel case still only works from KYC_ANALYST stage
- [ ] Approval/Rejection still closes case correctly
- [ ] Comments and audit log still track all actions
- [ ] Discretionary actions still function

---

## Performance Considerations
- Escalation path is stored as JSON in CMMN variables (memory-based, no DB query)
- Conditional sentries are evaluated by Flowable engine (standard CMMN processing)
- No additional DB tables needed
- Audit logging follows existing pattern

---

## Known Limitations & Future Enhancements
1. **Current:** Escalation is one-way (cannot de-escalate)
2. **Current:** No escalation timeout/SLA tracking
3. **Future:** Could add escalation approval requirements
4. **Future:** Could add escalation reason templates
5. **Future:** Could add escalation statistics/dashboards
