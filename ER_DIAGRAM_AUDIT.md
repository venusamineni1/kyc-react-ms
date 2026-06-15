# ER Diagram Audit Report - KYC Platform

**Date**: June 14, 2026  
**Status**: ⚠️ **OUTDATED - Requires Updates**

---

## Summary

The ER diagram at `/Users/venusamineni/Documents/KYC_ER.drawio` is **partially outdated**. While core entities are present, several important tables and relationships from the actual implementation are **missing or incorrectly represented**.

---

## 🔴 CRITICAL MISSING ENTITIES

### 1. **RelatedParties & Related Tables** (Viewer Service)
**Status**: NOT IN ER DIAGRAM ❌

The schema includes full support for Related Parties (beneficial owners, legal representatives, power of attorney), but these are completely absent from the diagram.

**Tables Missing**:
- `RelatedParties` (BIGINT PK, ClientID FK, RelationType, Name fields, citizenship, status, etc.)
- `RelatedPartyAddresses` (AddressID PK, RelatedPartyID FK, address fields)
- `RelatedPartyIdentifiers` (IdentifierID PK, RelatedPartyID FK, identifier fields)

**Impact**: Cannot properly document the client hierarchy and beneficial owner relationships.

**Action**: Add these three tables to the Viewer Service section with proper relationships.

---

### 2. **Batch Processing Tables** (Screening Service)
**Status**: PARTIALLY IN DIAGRAM ⚠️

The ER diagram shows basic screening, but critical batch processing tables are **missing or incomplete**.

**Tables Missing/Incomplete**:

#### Missing Entirely:
- `BatchRuns` (Core batch processing tracking)
  - BatchID, BatchName, RunStatus, NotificationStatus, FeedbackCount, CreatedAt, UpdatedAt
  - **FK**: MappingSnapshotID, ClientCount, RunGroupId

- `BatchRunErrors` (Error tracking for batch operations)
  - ErrorID PK, BatchID FK, RecordID, ErrorCode, ErrorMessage

- `BatchFeedbackResults` (Results from provider feedback)
  - ResultID PK, BatchID FK, RecordID, MatchID, MatchName, MatchScore, Status

- `MappingConfigs` (Field mapping definitions)
  - MappingID PK, TargetPath, SourceField, DefaultValue, Transformation

- `MappingConfigSnapshots` (Versioning of mapping configs)
  - SnapshotID PK, VersionLabel, CreatedAt, CreatedBy, Source, ConfigJson

- `BatchScreeningRuns` (Mass screening run tracking)
  - RunGroupId (UNIQUE), FileName, SystemId, TotalClientCount, TotalBatches
  - BatchesCompleted, OverallStatus, PersistClients, CallbackWebhookUrl, ErrorMessage

- `BatchScreeningStaging` (Batch client data staging)
  - Full client demographic data for persist-first mode
  - RunGroupId, ClientId, all address/identifier fields, ProcessingStatus

**Impact**: Cannot represent batch processing pipelines, error handling, or mass screening operations.

**Action**: Add these 7 tables to Screening Service section with proper relationships.

---

### 3. **Risk Batch Processing** (Risk Service)
**Status**: PARTIALLY IN DIAGRAM ⚠️

Risk Service has batch operations similar to screening but not clearly documented.

**Tables Missing**:
- `RiskMappings` (Field mappings for risk assessment)
  - MappingID PK, TargetPath, SourceField, DefaultValue, Category

- `BatchRuns` (Risk service batch processing)
  - BatchID PK, BatchName, RunStatus, NotificationStatus, FeedbackCount, CreatedAt, UpdatedAt

**Impact**: Cannot show risk batch pipeline.

**Action**: Add RiskMappings and clarify that BatchRuns exists in both screening and risk services.

---

## 🟡 INCOMPLETE TABLES (Existing but Missing Fields)

### 1. **Screening Service - ScreeningLogs**
**Diagram Shows**: ClientID, RequestPayload, ResponsePayload, OverallStatus  
**Actually Has**: ↑ PLUS:
- `NrtsProcessId` (BIGINT) - NRTS system process tracking
- `ExternalRequestID` (VARCHAR 100) - External system request ID
- `SourceType` (VARCHAR 50, DEFAULT 'MANUAL') - Manual vs batch request source

**Action**: Update ScreeningLogs table definition to include these fields.

---

### 2. **Screening Service - ScreeningResults**
**Diagram Shows**: ContextType, Status, AlertStatus, AlertMessage, AlertID  
**Actually Has**: ↑ PLUS:
- `NrtsReqId` (BIGINT) - NRTS request tracking

**Action**: Add NrtsReqId field to ScreeningResults.

---

### 3. **Viewer Service - CaseComments**
**Diagram Shows**: CommentID PK, CaseID FK, CommentText, CommentDate  
**Actually Has**: ↑ PLUS:
- `UserID` (VARCHAR 50) - Comment author ID
- `Role` (VARCHAR 50) - Author role at time of comment

**Action**: Update CaseComments to include UserID and Role fields.

---

### 4. **Viewer Service - CaseDocuments**
**Status**: MISSING FROM DIAGRAM ❌

Critical table for document management exists but not shown:
- DocumentID (BIGINT PK)
- CaseID (BIGINT FK)
- DocumentName, Category, MimeType, UploadedBy
- Comment (TEXT)
- Data (BLOB) - Binary document storage
- UploadDate (TIMESTAMP)

**Action**: Add CaseDocuments table to Viewer Service.

---

### 5. **Viewer Service - CaseEvents**
**Status**: MISSING FROM DIAGRAM ❌

Audit trail table for case lifecycle events:
- EventID (BIGINT PK)
- CaseID (BIGINT FK)
- EventType (VARCHAR 50: RISK_CHANGED, SCREENING_HIT, NEW_ONBOARDING, OTHER)
- EventDescription (TEXT)
- EventDate (TIMESTAMP)
- EventSource (VARCHAR 50: CRRE, SYSTEM, USER)

**Action**: Add CaseEvents table to Viewer Service.

---

### 6. **Viewer Service - Accounts**
**Status**: MISSING FROM DIAGRAM ❌

Client account tracking:
- AccountID (BIGINT PK)
- ClientID (BIGINT FK)
- AccountNumber (VARCHAR 50)
- AccountStatus (VARCHAR 20)

**Action**: Add Accounts table to Viewer Service.

---

### 7. **Viewer Service - Portfolios**
**Status**: MISSING FROM DIAGRAM ❌

Portfolio management:
- PortfolioID (BIGINT PK)
- ClientID (BIGINT FK)
- AccountNumber (VARCHAR 50)
- PortfolioText (VARCHAR 255)
- OnboardingDate, OffboardingDate
- Status (VARCHAR 20)

**Action**: Add Portfolios table to Viewer Service.

---

### 8. **Questionnaire Tables**
**Status**: MISSING FROM DIAGRAM ❌

Three related tables for questionnaire management:

**QuestionnaireSections**:
- SectionID (BIGINT PK)
- SectionName (VARCHAR 255)
- DisplayOrder (INT)

**QuestionnaireQuestions**:
- QuestionID (BIGINT PK)
- SectionID (BIGINT FK)
- QuestionText (TEXT)
- QuestionType (VARCHAR 20: TEXT, YES_NO, MULTI_CHOICE)
- IsMandatory (BOOLEAN)
- Options (TEXT - comma-separated for MULTI_CHOICE)
- DisplayOrder (INT)
- RiskFactorKey (VARCHAR 50: ADVERSE_MEDIA, SUSPICIOUS_ACTIVITY_REPORT, INVESTOR_VISA)

**CaseQuestionnaireResponses**:
- ResponseID (BIGINT PK)
- CaseID (BIGINT FK)
- QuestionID (BIGINT FK)
- AnswerText (TEXT)
- UNIQUE (CaseID, QuestionID)

**Action**: Add all three questionnaire tables with relationships.

---

### 9. **Viewer Service - MaterialChangeConfigs & MaterialChanges**
**Status**: PARTIALLY IN DIAGRAM ⚠️

**MaterialChangeConfigs** (Missing from Diagram):
- ConfigID (BIGINT PK)
- EntityName (VARCHAR 100)
- ColumnName (VARCHAR 100)
- Category (VARCHAR 20: SCREENING, RISK, BOTH, NONE)
- UNIQUE (EntityName, ColumnName)

**MaterialChanges** (Missing from Diagram):
- ChangeID (BIGINT PK)
- ChangeDate (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
- ClientID (BIGINT FK)
- EntityID (BIGINT)
- EntityName (VARCHAR 255)
- ColumnName (VARCHAR 255)
- OperationType (VARCHAR 50)
- OldValue (TEXT)
- NewValue (TEXT)
- Status (VARCHAR 20 DEFAULT 'PENDING')
- Category (VARCHAR 20)

**Action**: Add both tables to show material change audit trail.

---

## 🟢 CORRECT IN DIAGRAM

✅ **Auth Service Tables** (Correct):
- AppUsers
- RolePermissions
- UserAudits

✅ **Viewer Service Core Tables** (Correct):
- Clients (with all fields)
- ClientAddresses
- ClientIdentifiers
- Cases
- CaseComments (incomplete fields but structure correct)
- RiskAssessmentLogs
- RiskAssessments
- RiskAssessmentDetails
- ScreeningLogs
- ScreeningResults

---

## Summary of Changes Needed

| Table | Service | Status | Action |
|-------|---------|--------|--------|
| RelatedParties | Viewer | ❌ Missing | Add |
| RelatedPartyAddresses | Viewer | ❌ Missing | Add |
| RelatedPartyIdentifiers | Viewer | ❌ Missing | Add |
| CaseDocuments | Viewer | ❌ Missing | Add |
| CaseEvents | Viewer | ❌ Missing | Add |
| Accounts | Viewer | ❌ Missing | Add |
| Portfolios | Viewer | ❌ Missing | Add |
| QuestionnaireSections | Viewer | ❌ Missing | Add |
| QuestionnaireQuestions | Viewer | ❌ Missing | Add |
| CaseQuestionnaireResponses | Viewer | ❌ Missing | Add |
| MaterialChangeConfigs | Viewer | ❌ Missing | Add |
| MaterialChanges | Viewer | ❌ Missing | Add |
| BatchRuns | Screening | ⚠️ Missing | Add |
| BatchRunErrors | Screening | ⚠️ Missing | Add |
| BatchFeedbackResults | Screening | ⚠️ Missing | Add |
| MappingConfigs | Screening | ⚠️ Missing | Add |
| MappingConfigSnapshots | Screening | ⚠️ Missing | Add |
| BatchScreeningRuns | Screening | ⚠️ Missing | Add |
| BatchScreeningStaging | Screening | ⚠️ Missing | Add |
| RiskMappings | Risk | ⚠️ Missing | Add |
| BatchRuns | Risk | ⚠️ Missing | Add |
| ScreeningLogs (fields) | Screening | ⚠️ Incomplete | Update fields |
| ScreeningResults (fields) | Screening | ⚠️ Incomplete | Update fields |
| CaseComments (fields) | Viewer | ⚠️ Incomplete | Update fields |

**Total Missing**: 20 tables  
**Total Incomplete**: 4 tables

---

## Recommendations

### Priority 1 (Critical - Document Requirements):
1. Add all Questionnaire tables (required for case workflow)
2. Add MaterialChanges tables (required for compliance audit trail)
3. Add CaseDocuments and CaseEvents (core case management)
4. Add batch processing tables (required for screening pipeline)

### Priority 2 (Important - Data Relationships):
1. Add RelatedParties and supporting tables
2. Add Accounts and Portfolios
3. Add RiskMappings and Risk BatchRuns

### Priority 3 (Complete - Field Accuracy):
1. Update field lists for ScreeningLogs, ScreeningResults, CaseComments
2. Add missing fields to all documented tables

---

## Notes for Diagram Update

- **Color Scheme**: Keep existing (Blue for Auth, Green for Viewer/Core)
- **New Services**: Consider adding distinct colors for Screening Service and Risk Service if they're separate swimlanes
- **Relationships**: Clearly show all FK relationships with 1:N cardinality notation
- **Key Fields**: Continue using 🔑 for PKs and 🔗 for FKs
- **Service Boundaries**: Use swimlane boundaries to separate services
- **Field Annotations**: Include NOT NULL, DEFAULT values, and UNIQUE constraints where relevant

---

## SQL Migration Notes

If the diagram is updated to match current schema:
- Total of ~35+ tables across 4 services (Auth, Viewer, Screening, Risk)
- Multiple inter-service relationships via foreign keys
- Heavy audit trail requirements (UserAudits, CaseEvents, MaterialChanges)
- Complex batch processing with versioning (MappingConfigSnapshots, BatchScreeningRuns)

---

## Files Referenced

- ER Diagram: `/Users/venusamineni/Documents/KYC_ER.drawio`
- Auth Schema: `/Users/venusamineni/Projects/onekyc/kyc-react-ms/auth-service/src/main/resources/schema.sql`
- Viewer Schema: `/Users/venusamineni/Projects/onekyc/kyc-react-ms/viewer/src/main/resources/schema.sql`
- Screening Schema: `/Users/venusamineni/Projects/onekyc/kyc-react-ms/screening-service/src/main/resources/schema.sql`
- Risk Schema: `/Users/venusamineni/Projects/onekyc/kyc-react-ms/risk-service/src/main/resources/schema.sql`

---

**Report Generated**: June 14, 2026  
**Status**: ER Diagram requires **significant updates** to accurately represent current implementation
