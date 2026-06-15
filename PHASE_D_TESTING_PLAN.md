# Phase D: Testing & Documentation Plan

## Overview
Phase D consists of comprehensive testing of the real service integrations, format transformations, and verification that the system works correctly with both real and mock modes.

## Prerequisites

1. **Set correct JAVA_HOME** (macOS JDK bundle):
   ```bash
   export JAVA_HOME="/Users/venusamineni/Projects/onekyc/kyc-react-ms/jdk-21.0.2+13/Contents/Home"
   ```

2. **Build all modules:**
   ```bash
   ./gradlew build -x test
   ```

3. **Optional: Clean Gradle daemon** if compilation issues:
   ```bash
   rm -rf .gradle/daemon
   ./gradlew build -x test
   ```

---

## Test 1: Real Services Mode (Default)

### Setup
```bash
# Start all services with real service defaults
./start-all.sh
```

Services will start in this order:
1. Eureka (port 8761)
2. Auth Service (port 8084)
3. Risk Service (port 8081)
4. Screening Service (port 8082)
5. Document Service (port 8085)
6. KYC Orchestration (port 8086)
7. Viewer Service (port 8083)
8. API Gateway (port 8080)
9. Frontend (port 5173)

### Expected Behavior

#### 1.1 Pre-Check Flow (New Prospect Creation)

**Steps:**
1. Access frontend: http://localhost:5173
2. Create new prospect (ProspectController triggers KYC pre-check)
3. Observe API calls and data persistence

**Verify:**
- [ ] Orchestration service receives request
- [ ] Log file shows: "Calling RealScreeningClient for screening service"
- [ ] Log file shows screening-service call successful
- [ ] Log file shows: "Screening result transformed: hit=..."
- [ ] Log file shows: "Calling RealRiskClient for risk service"
- [ ] Log file shows risk-service call successful
- [ ] Log file shows: "Risk result transformed: rating=..."
- [ ] Frontend displays Case Details with screening/risk results
- [ ] Screening results show in Case Details → Risk & Screening tab
- [ ] Risk results show with diagram (25 LOW RISK, etc)
- [ ] ScreeningLog record created with sourceType="KYC_ORCHESTRATION_PRECHECK"
- [ ] RiskAssessment record created with typeOfLogicApplied showing pre-check source

**Expected Database State:**
```sql
-- ScreeningLog entry for pre-check
SELECT logID, clientID, sourceType, externalRequestID FROM ScreeningLogs 
WHERE sourceType='KYC_ORCHESTRATION_PRECHECK' 
ORDER BY createdAt DESC LIMIT 1;

-- RiskAssessment entry for pre-check
SELECT * FROM RiskAssessments 
WHERE typeOfLogicApplied='KYC_ORCHESTRATION_PRECHECK' 
ORDER BY createdAt DESC LIMIT 1;
```

#### 1.2 Manual Screening Flow (Analyst Workflow)

**Steps:**
1. Navigate to Case Details page
2. Click ScreeningPanel → "Run Screening"
3. Observe screening executes independently

**Verify:**
- [ ] Screening runs independently
- [ ] Log shows: "NrtsScreeningProvider" call (not "MockScreeningProvider")
- [ ] Log shows NRTS API calls in screening-service logs
- [ ] Risk is NOT automatically triggered (per user request)
- [ ] ScreeningLog.sourceType="MANUAL"
- [ ] Results display in Risk & Screening tab
- [ ] Pre-check badge NOT shown (only shows for pre-check sources)

**Expected Database State:**
```sql
-- ScreeningLog entry for manual screening
SELECT logID, clientID, sourceType FROM ScreeningLogs 
WHERE sourceType='MANUAL' 
ORDER BY createdAt DESC LIMIT 1;

-- Verify risk was NOT auto-created
SELECT COUNT(*) FROM RiskAssessments 
WHERE clientID = <client_id> AND createdAt > <screening_time>;
-- Should return 0 or same count as before screening
```

#### 1.3 Verify Format Transformation

**In logs, look for:**

Screening transformation:
```
Calling RealScreeningClient for screening service
Screening result transformed: hit=Hit, contexts=[PEP, INT]
```

Risk transformation:
```
Calling RealRiskClient for risk service
Risk result transformed: rating=HIGH
```

---

## Test 2: Mock Services Mode (Development Testing)

### Setup
```bash
# Stop existing services
killall java 2>/dev/null || true
sleep 3

# Start with mock services enabled
export SCREENING_MOCK=true
export RISK_MOCK=true
export NRTS_MOCK=true

./start-all.sh
```

### Expected Behavior

#### 2.1 Pre-Check Flow with Mocks

**Steps:**
1. Create new prospect

**Verify:**
- [ ] Log shows: "Calling MockScreeningClient"
- [ ] Log shows: "Calling MockRiskClient"
- [ ] No HTTP calls to screening-service or risk-service in logs
- [ ] Results still display correctly (format transformation still applied)
- [ ] ScreeningLog.sourceType="KYC_ORCHESTRATION_PRECHECK"
- [ ] Data matches mock hardcoded values

**Verify Format Transformation Still Works:**
```
MockScreeningClient returns: ScreeningResult(hit=Hit, hitContext=[PEP])
Orchestration persists: ScreeningResult(hit=Hit, hitContext=[PEP])
Frontend displays: Screening hit results with PEP context
```

#### 2.2 Manual Screening with Mock

**Steps:**
1. Navigate to Case Details
2. Run manual screening

**Verify:**
- [ ] Log shows: "MockScreeningProvider" (not "NrtsScreeningProvider")
- [ ] Results return immediately (no NRTS delay)
- [ ] ScreeningLog.sourceType="MANUAL"
- [ ] Results match mock data structure

---

## Test 3: Mixed Mode (Partial Real, Partial Mock)

### Setup
```bash
# Use real screening, mock risk
export SCREENING_MOCK=false
export RISK_MOCK=true
export NRTS_MOCK=false

./start-all.sh
```

### Expected Behavior

**Verify:**
- [ ] Pre-check calls real screening-service, mock risk-service
- [ ] Log shows: "Calling RealScreeningClient" + "Calling MockRiskClient"
- [ ] Screening results from real NRTS
- [ ] Risk results from mock
- [ ] Both datasets display correctly

---

## Test 4: API Key Authentication

### Setup
Verify that `/api/internal/**` endpoints require API key header.

**Test 4.1: Call without API key**
```bash
curl -X POST http://localhost:8086/api/internal/kyc/initiate \
  -H "Content-Type: application/json" \
  -d '{"prospectId": 1}'
```

**Expected:** 403 Forbidden

**Test 4.2: Call with invalid API key**
```bash
curl -X POST http://localhost:8086/api/internal/kyc/initiate \
  -H "Content-Type: application/json" \
  -H "X-Internal-Api-Key: invalid-key" \
  -d '{"prospectId": 1}'
```

**Expected:** 403 Forbidden

**Test 4.3: Call with valid API key**
```bash
curl -X POST http://localhost:8086/api/internal/kyc/initiate \
  -H "Content-Type: application/json" \
  -H "X-Internal-Api-Key: dev-internal-kyc-key-change-in-prod" \
  -d '{"prospectId": 1}'
```

**Expected:** 200 OK with orchestration response

---

## Test 5: Frontend Display Verification

### Test 5.1: Source Type Badges

**Navigate to Case Details (case created via pre-check):**

**Verify:**
- [ ] Risk section shows badge: "Pre-check" (blue color)
- [ ] Risk shows: "Last assessed: <date> Pre-check"
- [ ] Screening section shows badge (if applicable)

**Navigate to Case Details (case created via manual screening):**

**Verify:**
- [ ] Risk section shows no badge (if not from pre-check)
- [ ] Screening section shows no badge or correct source

### Test 5.2: Screening History Modal

**Steps:**
1. Click ScreeningPanel → "View History"
2. Modal opens showing past screenings

**Verify:**
- [ ] Pre-check screenings show "Pre-check" badge (blue)
- [ ] Manual screenings show no badge or "Manual" badge
- [ ] Each entry shows date, result, and source
- [ ] "Analyze" drill-down works for each entry

### Test 5.3: Risk History / Details

**Steps:**
1. Click Risk assessment → "View Details"
2. Should show full risk assessment details

**Verify:**
- [ ] Source type visible (Pre-check vs Manual)
- [ ] All risk data displays correctly
- [ ] Links to corresponding screening log work

---

## Test 6: Error Scenarios

### Test 6.1: Service Unavailable

**Setup:**
1. Stop screening-service: `lsof -ti:8082 | xargs kill -9`
2. Attempt to create new prospect (triggers pre-check)

**Expected:**
- [ ] Orchestration logs error
- [ ] Error message propagates to frontend
- [ ] Database transaction rolls back (no partial records)
- [ ] Frontend shows error notification

**Verify:**
```
Log: "Failed to call screening service: Connection refused"
Frontend: Error notification about screening service unavailability
Database: No new ScreeningLog or RiskAssessment records created
```

### Test 6.2: Malformed Response

**Setup:**
Mock a malformed response from screening-service (manually or via test stub)

**Expected:**
- [ ] RealScreeningClient logs parsing error
- [ ] Orchestration handles gracefully
- [ ] Frontend shows user-friendly error

---

## Test 7: Performance & Logging

### Test 7.1: Verify Logging Output

**Check logs while running pre-check:**
```bash
tail -f logs/kyc-orchestration.log | grep -i "screening\|risk"
```

**Expected to see:**
```
[timestamp] INFO  c.v.k.o.client.RealScreeningClient - Calling RealScreeningClient for screening service
[timestamp] INFO  c.v.k.o.client.RealScreeningClient - Screening result transformed: hit=Hot, contexts=[PEP, INT]
[timestamp] INFO  c.v.k.o.client.RealRiskClient - Calling RealRiskClient for risk service
[timestamp] INFO  c.v.k.o.client.RealRiskClient - Risk result transformed: rating=HIGH
```

### Test 7.2: Response Times

**Measure end-to-end pre-check time:**
- Log orchestration service entry time
- Log orchestration service exit time
- Calculate total (should include screening + risk service latency)

**Typical times (with real services):**
- Screening service: 500ms - 2s (includes NRTS latency)
- Risk service: 100ms - 500ms
- Total pre-check: 1s - 3s

---

## Test 8: Database Integrity

### Test 8.1: Verify Data Persistence

**After running pre-check and manual screening:**

```sql
-- Count pre-check vs manual screenings
SELECT sourceType, COUNT(*) as count 
FROM ScreeningLogs 
GROUP BY sourceType;

-- Verify all ScreeningLog entries have matching ScreeningLog.resultID
SELECT COUNT(*) FROM ScreeningLogs sl
WHERE NOT EXISTS (SELECT 1 FROM ScreeningResults sr WHERE sr.logID = sl.logID);
-- Should return 0

-- Verify RiskAssessment has correct timestamps
SELECT clientID, typeOfLogicApplied, createdAt 
FROM RiskAssessments 
ORDER BY createdAt DESC 
LIMIT 5;
```

### Test 8.2: Verify No Duplicates

**Create same prospect twice, verify sourceType recorded correctly:**

```sql
SELECT DISTINCT sourceType FROM ScreeningLogs 
ORDER BY sourceType;
-- Should show: KYC_ORCHESTRATION_PRECHECK, MANUAL
```

---

## Rollback / Troubleshooting

### If Real Service Calls Fail

**Option 1: Check service is running**
```bash
lsof -i :8082  # Check screening-service on port 8082
lsof -i :8081  # Check risk-service on port 8081
```

**Option 2: Check logs**
```bash
tail -50 logs/kyc-orchestration.log | grep -i error
tail -50 logs/screening-service.log | grep -i error
tail -50 logs/risk-service.log | grep -i error
```

**Option 3: Switch to mock mode temporarily**
```bash
export SCREENING_MOCK=true
export RISK_MOCK=true
# Restart orchestration service
```

### If Frontend Shows No Results

**Check:**
1. Verify ScreeningLog and RiskAssessment records exist in database
2. Check viewer service is fetching latest data correctly
3. Check API gateway is properly routing requests

```bash
# Check database
sqlite3 /tmp/kycorchestrationdb.db "SELECT * FROM ScreeningLogs ORDER BY createdAt DESC LIMIT 1;"

# Check logs
tail -20 logs/viewer.log | grep -i screening
```

---

## Sign-Off Checklist

After all tests pass:

- [ ] Pre-check flow uses real services by default
- [ ] Manual screening uses real NRTS by default
- [ ] Format transformation works correctly
- [ ] Source type tracking works (MANUAL vs KYC_ORCHESTRATION_PRECHECK)
- [ ] Frontend displays source type badges correctly
- [ ] Mock mode works when enabled
- [ ] Mixed mode works (partial real, partial mock)
- [ ] API key authentication enforced on internal endpoints
- [ ] Error handling works gracefully
- [ ] Database integrity maintained
- [ ] Logs are clear and helpful for debugging
- [ ] Performance is acceptable (pre-check < 3s typically)

---

## Documentation Updates Needed

1. **API Documentation**: Update Swagger descriptions to reflect real service defaults
2. **Deployment Guide**: Document configuration flags for staging/prod
3. **Troubleshooting Guide**: Add section on format transformation errors
4. **Architecture Diagram**: Update to show RealScreeningClient and RealRiskClient
