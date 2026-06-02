# Screening Service Error Fix - 500 Internal Server Error

## Problem Summary
When running screening from case details, the API returns **500 Internal Server Error** with message:
```
Error initiating screening: Failed to call Screening Service
```

## Root Cause Analysis

### Frontend Issue (ScreeningPanel.jsx:191)
```javascript
// Current (WRONG):
const res = await screeningService.initiateScreening(clientId);
// Calls: POST /screening/initiate/1  ← clientId only, no body
```

### Backend Expectation (ScreeningController.java:33-37)
```java
@PostMapping("/initiate")
public ResponseEntity<?> initiateScreening(
    @RequestBody ScreeningDTOs.ScreeningInternalRequest request) {
    // Expects request body with:
    // - clientId (Long)
    // - firstName (String)
    // - lastName (String)  
    // - dateOfBirth (String)
    // - citizenship (String)
    // - statusCheckDelayMs (Long, optional)
}
```

### Issues
1. **Missing client details**: Frontend only sends `clientId`, backend needs full client object
2. **Wrong endpoint path**: Frontend uses `/screening/initiate` but backend is at `/api/internal/screening/initiate`
3. **Wrong HTTP format**: Frontend sends clientId as path param, backend expects request body
4. **No proxy/gateway routing**: The API routing appears incorrect

---

## Solution

### Option 1: Fix Frontend to Send Proper Request Body (RECOMMENDED)

#### Step 1: Update screeningService.js
```javascript
// File: viewer/frontend/src/services/screeningService.js

export const screeningService = {
    initiateScreening: async (clientDetails) => {
        // clientDetails should be: { clientId, firstName, lastName, dateOfBirth, citizenship }
        return apiClient.post('/api/internal/screening/initiate', clientDetails);
    },
    
    getScreeningStatus: async (processId) => {
        return apiClient.get(`/api/internal/screening/status/${processId}`);
    },
    
    getHistory: async (clientId) => {
        return apiClient.get(`/api/internal/screening/history/${clientId}`);
    }
};
```

#### Step 2: Update ScreeningPanel.jsx
Pass full client details instead of just clientId:

```javascript
// File: viewer/frontend/src/components/ScreeningPanel.jsx

const ScreeningPanel = ({ 
    clientId, 
    clientData,  // ADD: Pass full client object from parent
    hasPermission 
}) => {
    // ... existing code ...

    const runScreening = async () => {
        if (!hasPermission) return;
        try {
            // Build screening request with full client details
            const screeningRequest = {
                clientId: clientId,
                firstName: clientData?.firstName || '',
                lastName: clientData?.lastName || '',
                dateOfBirth: clientData?.dateOfBirth || '',
                citizenship: clientData?.citizenship || '',
                statusCheckDelayMs: 0  // Immediate status check
            };
            
            const res = await screeningService.initiateScreening(screeningRequest);
            setCurrentRequestId(res.requestId || res.processId);
            setStatus('IN_PROGRESS');
            // ... rest of existing code ...
        } catch (e) {
            notify('Failed to start screening: ' + e.message, 'error');
            setStatus('NOT_RUN');
        }
    };

    // ... rest of component ...
};
```

#### Step 3: Update parent component (CaseDetails.jsx)
Pass client data to ScreeningPanel:

```javascript
// Before (or check what component renders ScreeningPanel):
<ScreeningPanel clientId={caseId} hasPermission={canScreening} />

// After:
<ScreeningPanel 
    clientId={caseId}
    clientData={clientDetails}  // ADD: pass full client object
    hasPermission={canScreening} 
/>
```

---

### Option 2: Add Backend Endpoint That Accepts ClientId Only

If you want to keep the frontend as-is, create a new backend endpoint:

```java
// Add to ScreeningController.java
@PostMapping("/initiate/{clientId}")
public ResponseEntity<?> initiateScreeningByClientId(
        @PathVariable Long clientId) {
    try {
        // Fetch client details from database
        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        
        ScreeningDTOs.ScreeningInternalRequest request = 
            new ScreeningDTOs.ScreeningInternalRequest(
                clientId,
                client.getFirstName(),
                client.getLastName(),
                client.getDateOfBirth().toString(),
                client.getCitizenship(),
                0L
            );
        
        return ResponseEntity.ok(service.initiateScreening(request));
    } catch (Exception e) {
        return ResponseEntity.internalServerError()
            .body("Error initiating screening: " + e.getMessage());
    }
}
```

Then update frontend base URL from `/screening/initiate` to `/api/internal/screening/initiate`.

---

## Recommended Fix Path

1. **Option 1 (RECOMMENDED)** - Update frontend to send full client details
   - More RESTful design
   - Follows backend API contract
   - Better separation of concerns
   - Estimated effort: 30 minutes

2. **Option 2** - Add new backend endpoint if frontend refactor is not feasible
   - Requires database access in controller
   - Adds API bloat
   - Estimated effort: 20 minutes + testing

---

## API Endpoint Correction

**Correct endpoint path:**
```
POST /api/internal/screening/initiate
Content-Type: application/json

{
  "clientId": 1,
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-15",
  "citizenship": "US",
  "statusCheckDelayMs": 0
}

Response 200:
{
  "result": "Hot" | "No-Hit",
  "processId": 12345,
  "reqId": 67890,
  "alertContexts": ["PEP", "INT"]
}
```

---

## Testing Checklist

After fix:
- [ ] Can initiate screening from case details
- [ ] Screening status updates in real-time (polling every 2 seconds)
- [ ] Screening history loads correctly
- [ ] Alert details display when "Analyze" is clicked
- [ ] No 500 errors in browser console
- [ ] Response contains correct processId and alertContexts

---

## Files to Modify
1. `viewer/frontend/src/services/screeningService.js` - Fix endpoint paths
2. `viewer/frontend/src/components/ScreeningPanel.jsx` - Pass full client details
3. Parent component (CaseDetails.jsx or similar) - Pass clientData prop

Optional:
4. `screening-service/.../ScreeningController.java` - Add clientId-only endpoint if needed
