# Risk Service — API Reference

**Base URL:** `http://<host>:8081`  
**Content-Type:** `application/json`  
**Service Name (Eureka):** `risk-service`  
**Auth:** All `/api/internal/**` endpoints require the header `X-Internal-Api-Key: <key>`. Requests without a valid key are rejected with `403 Forbidden`.

---

## Table of Contents

**Real-Time Risk Assessment**

1. [POST /api/internal/risk/calculate](#1-post-apiinternalriskcalculate)
2. [GET /api/internal/risk/logs](#2-get-apiinternalrisklogs)
3. [GET /api/internal/risk/assessments](#3-get-apiinternalriskassessments)
4. [GET /api/internal/risk/assessments/{recordId}](#4-get-apiinternalriskassessmentsrecordid)
5. [GET /api/internal/risk/assessment-details/{assessmentId}](#5-get-apiinternalriskassessment-detailsassessmentid)

**Batch Risk Processing**

6. [POST /api/internal/risk/batch/initiate](#6-post-apiinternalriskbatchinitiate)
7. [POST /api/internal/risk/batch/create](#7-post-apiinternalriskbatchcreate)
8. [POST /api/internal/risk/batch/{batchId}/generate-jsonl](#8-post-apiinternalriskbatchbatchidgenerate-jsonl)
9. [POST /api/internal/risk/batch/{batchId}/zip](#9-post-apiinternalriskbatchbatchidzippx)
10. [POST /api/internal/risk/batch/{batchId}/generate-control](#10-post-apiinternalriskbatchbatchidgenerate-control)
11. [POST /api/internal/risk/batch/{batchId}/upload](#11-post-apiinternalriskbatchbatchidupload)
12. [GET /api/internal/risk/batch/history](#12-get-apiinternalriskbatchhistory)
13. [GET /api/internal/risk/batch/{batchId}/file-content](#13-get-apiinternalriskbatchbatchidfile-content)
14. [GET /api/internal/risk/batch/mapping](#14-get-apiinternalriskbatchmapping)
15. [POST /api/internal/risk/batch/mapping](#15-post-apiinternalriskbatchmapping)
16. [POST /api/internal/risk/batch/test-generate](#16-post-apiinternalriskbatchtest-generate)

**Reference**

17. [Data Dictionaries](#17-data-dictionaries)
18. [Error Responses](#18-error-responses)
19. [Architecture & Security Notes](#19-architecture--security-notes)

---

## 1. POST /api/internal/risk/calculate

Submits a risk rating request to the CRRE (Client Risk Rating Engine) for one or more clients. The service stores each result as a `RiskAssessment` plus per-pillar `RiskAssessmentDetail` rows, and returns the full CRRE response synchronously.

### Request

```http
POST /api/internal/risk/calculate
Content-Type: application/json
X-Internal-Api-Key: <key>
```

#### Request Body — `CalculateRiskRequest`

| Field | Type | Required | Description |
|---|---|---|---|
| `header` | `object` | **Yes** | Request metadata forwarded to CRRE. |
| `clientRiskRatingRequest` | `array<ClientRiskRatingRequestItem>` | **Yes** | One item per client to rate. Batch calls may include multiple items. |

##### `header`

| Field | Type | Required | Description |
|---|---|---|---|
| `callerSystem` | `string` | **Yes** | Identifier of the calling system (e.g. `KYC-SERVICE`). |
| `dbBusinessline` | `string` | No | Business line code (e.g. `EIS`, `PPR`). |
| `crrmVersion` | `string` | No | CRRE model version to invoke (e.g. `2.0`). |
| `requestID` | `string` | No | Caller-supplied correlation ID. |
| `requestTimeStamp` | `string` | No | ISO-8601 timestamp of the request. |

##### `ClientRiskRatingRequestItem`

| Field | Type | Required | Description |
|---|---|---|---|
| `clientDetails` | `object` | **Yes** | Client identity and adoption metadata. |
| `entityRiskType` | `object` | No | KYC legal entity classification. |
| `industryRiskType` | `object` | No | Industry/occupation risk inputs. |
| `geoRiskType` | `object` | No | Geographic risk inputs — countries, addresses, related parties. |
| `productRiskType` | `array<object>` | No | One entry per product held by the client. |
| `channelRiskType` | `object` | No | Onboarding / distribution channel. |

##### `clientDetails`

| Field | Type | Required | Description |
|---|---|---|---|
| `recordID` | `string` | **Yes** | Client record identifier. Used to correlate response items and stored on `RiskAssessment`. |
| `clientAdoptionCountry` | `string` | No | Country where the client was adopted/onboarded (ISO 3166-1 alpha-2). High-risk countries (e.g. `CU`) affect the final rating. |
| `countryOfDomicile` | `string` | No | Country of domicile (ISO 3166-1 alpha-2). |
| `investorVisa` | `boolean` | No | `true` if the client holds an investor visa. |
| `defenceRevenue` | `string` | No | Defence revenue indicator. |
| `smeAssessment` | `string` | No | SME assessment code. |
| `smeRiskAssessment` | `string` | No | SME risk assessment outcome. |
| `additionalRule` | `array<AdditionalRule>` | No | Free-form key/value risk rules. |

##### `AdditionalRule`

| Field | Type | Description |
|---|---|---|
| `ruleType` | `string` | Rule category identifier. |
| `question` | `string` | The rule question text. |
| `response` | `string` | The answer/value for the rule. |

##### `entityRiskType`

| Field | Type | Description |
|---|---|---|
| `typeKYCLegalEntityCode` | `string` | KYC legal entity code (e.g. `NP4` for natural person). |

##### `industryRiskType`

| Field | Type | Description |
|---|---|---|
| `occupationCode` | `array<string>` | One or more occupation codes (e.g. `["00101"]`). |

##### `geoRiskType`

| Field | Type | Description |
|---|---|---|
| `relatedParty` | `array<RelatedPartyRisk>` | Related party risk elements (e.g. beneficiary countries). |
| `partyAccount` | `array<PartyAccountRisk>` | Account-level geographic risk inputs. |
| `residentialAddressValidFrom` | `string` | Date from which the residential address is valid (`YYYY-MM-DD`). |

##### `RelatedPartyRisk`

| Field | Type | Description |
|---|---|---|
| `relatedPartyElement` | `string` | Element type identifier. |
| `relatedPartyElementValues` | `array<string>` | Country codes or other values for this element. |

##### `PartyAccountRisk`

| Field | Type | Description |
|---|---|---|
| `countryOfNationality` | `array<string>` | Nationality country codes. |
| `originOfFunds` | `array<string>` | Countries from which funds originate. |
| `dateOfResidence` | `array<string>` | Dates of residence (ISO dates as strings). |
| `addressType` | `AddressTypeRisk` | Address type breakdown. |

##### `AddressTypeRisk`

| Field | Type | Description |
|---|---|---|
| `postalAddress` | `array<string>` | Postal address country codes. |
| `clientDomicile` | `string` | Domicile country code. |

##### `productRiskType`

| Field | Type | Description |
|---|---|---|
| `productCode` | `string` | Product code (e.g. `OAP1`). |

##### `channelRiskType`

| Field | Type | Description |
|---|---|---|
| `channelCode` | `string` | Distribution channel code (e.g. `CHN05`). |

#### Example Request

```json
{
  "header": {
    "callerSystem": "KYC-SERVICE",
    "dbBusinessline": "EIS",
    "crrmVersion": "2.0",
    "requestID": "req-abc123",
    "requestTimeStamp": "2026-06-16T10:00:00+02:00"
  },
  "clientRiskRatingRequest": [
    {
      "clientDetails": {
        "recordID": "CLIENT-20260411-00123",
        "clientAdoptionCountry": "DE",
        "countryOfDomicile": "CH",
        "investorVisa": false
      },
      "entityRiskType": {
        "typeKYCLegalEntityCode": "NP4"
      },
      "industryRiskType": {
        "occupationCode": ["00101"]
      },
      "geoRiskType": {
        "partyAccount": [
          {
            "countryOfNationality": ["DE"],
            "originOfFunds": ["DE"],
            "dateOfResidence": ["2020-01-01"],
            "addressType": {
              "postalAddress": ["DE"],
              "clientDomicile": "CH"
            }
          }
        ],
        "residentialAddressValidFrom": "2022-03-01"
      },
      "productRiskType": [
        { "productCode": "OAP1" }
      ],
      "channelRiskType": {
        "channelCode": "CHN05"
      }
    }
  ]
}
```

### Response

**HTTP 200 OK** — `CalculateRiskResponse`

#### Response Body

| Field | Type | Description |
|---|---|---|
| `header` | `object` | Response metadata echoed/augmented by CRRE. Includes `responseTimeStamp`, `eventModelVersion`, `eventCalibrationVersion`, `eventModelRunInstance`. |
| `processStatus` | `object` | Overall batch processing outcome. |
| `clientRiskRatingResponse` | `array<ClientRiskRatingResponseItem>` | One item per input client, in the same order. |

##### `processStatus`

| Field | Type | Description |
|---|---|---|
| `crreStatus` | `string` | Top-level CRRE status string (e.g. `Success`). |
| `successfulRecords` | `integer` | Number of records rated successfully. |
| `errorRecords` | `integer` | Number of records that failed. |
| `warningRecords` | `integer` | Number of records rated with warnings. |

##### `ClientRiskRatingResponseItem`

| Field | Type | Description |
|---|---|---|
| `recordID` | `string` | Client record identifier from the request. |
| `riskRatingType` | `string` | Type of risk rating applied. |
| `clientAdoptionCountry` | `string` | Echoed adoption country. |
| `error` | `string` | Error detail if this record failed; `null` on success. |
| `overallRiskAssessment` | `object` | Aggregated risk outcome. |
| `entityRiskType` | `object` | Entity pillar result (includes `pillarScore`, `pillarRiskCategory`, `riskClassification`). |
| `industryRiskType` | `object` | Industry pillar result. |
| `geoRiskType` | `object` | Geo pillar result. |
| `productRiskType` | `object` | Product pillar result. |
| `channelRiskType` | `object` | Channel pillar result. |

##### `overallRiskAssessment`

| Field | Type | Description |
|---|---|---|
| `overallRiskScore` | `integer` | Numeric risk score (e.g. `1`–`9`). |
| `initialRiskLevel` | `string` | Initial computed level before SME override: `LOW`, `MEDIUM`, `HIGH`. |
| `riskRatingPreSMEAssessment` | `string` | Risk level prior to SME assessment. |
| `overallRiskLevel` | `string` | Final risk level after all rules: `LOW`, `MEDIUM`, `HIGH`. |
| `typeOfLogicApplied` | `string` | Logic type applied (e.g. `Standard`). |
| `smeRiskAssessment` | `string` | SME-driven override, if any. |
| `riskScoreDetails` | `string` | Extended score breakdown details. |

##### Pillar Fields (entity / industry / geo / product / channel)

Each pillar object in the response carries the following additional fields:

| Field | Type | Description |
|---|---|---|
| `pillarScore` | `integer` | Numeric score for this risk pillar. |
| `pillarRiskCategory` | `string` | Risk category for this pillar: `LOW`, `MEDIUM`, `HIGH`. |
| `typeOfLogicApplied` | `string` | Logic variant used for this pillar. |
| `riskClassification` | `array<RiskClassification>` | Individual element-level scores within the pillar. |

##### `RiskClassification`

| Field | Type | Description |
|---|---|---|
| `elementName` | `string` | Name of the risk element (e.g. `countryOfNationality`, `occupationCode`). |
| `elementValue` | `string` | Value that was evaluated (e.g. `DE`, `00101`). |
| `riskScore` | `integer` | Score assigned to this element. |
| `flag` | `string` | Override flag if applicable. |
| `regulatoryCRROverride` | `string` | Regulatory CRR override code. |
| `localRuleApplied` | `string` | Whether a local rule was applied (`Y`/`N`). |

#### Example Response

```json
{
  "header": {
    "callerSystem": "KYC-SERVICE",
    "crrmVersion": "2.0",
    "responseTimeStamp": "2026-06-16T10:00:00+02:00",
    "eventModelRunInstance": "114654-1",
    "eventModelVersion": "CRRE 22.2",
    "eventCalibrationVersion": "122"
  },
  "processStatus": {
    "crreStatus": "Success",
    "successfulRecords": 1,
    "errorRecords": 0,
    "warningRecords": 0
  },
  "clientRiskRatingResponse": [
    {
      "recordID": "CLIENT-20260411-00123",
      "clientAdoptionCountry": "DE",
      "error": null,
      "overallRiskAssessment": {
        "overallRiskScore": 1,
        "initialRiskLevel": "LOW",
        "riskRatingPreSMEAssessment": "LOW",
        "overallRiskLevel": "LOW",
        "typeOfLogicApplied": "Standard",
        "smeRiskAssessment": ""
      },
      "entityRiskType": {
        "pillarScore": 14,
        "pillarRiskCategory": "LOW",
        "riskClassification": [
          { "elementName": "typeKYCLegalEntityCode", "elementValue": "NP4", "riskScore": 14, "localRuleApplied": "N" }
        ]
      },
      "industryRiskType": {
        "pillarScore": 18,
        "pillarRiskCategory": "LOW",
        "riskClassification": [
          { "elementName": "occupationCode", "elementValue": "00101", "riskScore": 18, "localRuleApplied": "N" }
        ]
      },
      "geoRiskType": {
        "pillarScore": 12,
        "pillarRiskCategory": "LOW",
        "riskClassification": [
          { "elementName": "countryOfNationality", "elementValue": "DE", "riskScore": 12, "localRuleApplied": "N" }
        ]
      },
      "productRiskType": {
        "pillarScore": 10,
        "pillarRiskCategory": "LOW",
        "riskClassification": [
          { "elementName": "productCode", "elementValue": "OAP1", "riskScore": 10, "localRuleApplied": "N" }
        ]
      },
      "channelRiskType": {
        "pillarScore": 8,
        "pillarRiskCategory": "LOW",
        "riskClassification": [
          { "elementName": "channelCode", "elementValue": "CHN05", "riskScore": 8, "localRuleApplied": "N" }
        ]
      }
    }
  ]
}
```

---

## 2. GET /api/internal/risk/logs

Returns the full audit history of every `POST /api/internal/risk/calculate` call, including the raw request and response JSON payloads.

### Request

```http
GET /api/internal/risk/logs
X-Internal-Api-Key: <key>
```

### Response

**HTTP 200 OK** — `array<RiskAssessmentLog>`

| Field | Type | Description |
|---|---|---|
| `logID` | `long` | Auto-generated log record ID. |
| `requestJSON` | `string` | Raw JSON of the `CalculateRiskRequest`. |
| `responseJSON` | `string` | Raw JSON of the `CalculateRiskResponse`. |
| `status` | `string` | Outcome: `SUCCESS` or `ERROR`. |
| `createdAt` | `string` (ISO-8601) | Timestamp when the log entry was created. |

#### Example Response

```json
[
  {
    "logID": 1,
    "requestJSON": "{\"header\":{...},\"clientRiskRatingRequest\":[...]}",
    "responseJSON": "{\"header\":{...},\"processStatus\":{...},\"clientRiskRatingResponse\":[...]}",
    "status": "SUCCESS",
    "createdAt": "2026-06-16T10:00:00.123"
  }
]
```

---

## 3. GET /api/internal/risk/assessments

Returns all stored `RiskAssessment` records — the processed summary of each CRRE response item.

### Request

```http
GET /api/internal/risk/assessments
X-Internal-Api-Key: <key>
```

### Response

**HTTP 200 OK** — `array<RiskAssessment>`

| Field | Type | Description |
|---|---|---|
| `assessmentID` | `long` | Auto-generated assessment record ID. |
| `logID` | `long` | Foreign key to the `RiskAssessmentLog` that produced this record. |
| `recordID` | `string` | Client record identifier from the CRRE request. |
| `overallRiskScore` | `integer` | Aggregated numeric risk score. |
| `initialRiskLevel` | `string` | Risk level before SME override: `LOW`, `MEDIUM`, `HIGH`. |
| `overallRiskLevel` | `string` | Final risk level: `LOW`, `MEDIUM`, `HIGH`. |
| `typeOfLogicApplied` | `string` | Logic type used (e.g. `Standard`). |
| `smeRiskAssessment` | `string` | SME override value, if any. |
| `createdAt` | `string` (ISO-8601) | Timestamp when the assessment was stored. |

#### Example Response

```json
[
  {
    "assessmentID": 1,
    "logID": 1,
    "recordID": "CLIENT-20260411-00123",
    "overallRiskScore": 1,
    "initialRiskLevel": "LOW",
    "overallRiskLevel": "LOW",
    "typeOfLogicApplied": "Standard",
    "smeRiskAssessment": "",
    "createdAt": "2026-06-16T10:00:00.456"
  }
]
```

---

## 4. GET /api/internal/risk/assessments/{recordId}

Returns all `RiskAssessment` records for a specific client `recordID`. A client may have multiple assessments if rated more than once.

### Request

```http
GET /api/internal/risk/assessments/{recordId}
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `recordId` | `string` | **Yes** | The client record identifier (`clientDetails.recordID`). |

#### Example Request

```http
GET /api/internal/risk/assessments/CLIENT-20260411-00123
```

### Response

**HTTP 200 OK** — same schema as [GET /api/internal/risk/assessments](#3-get-apiinternalriskassessments), filtered to the given `recordId`.

---

## 5. GET /api/internal/risk/assessment-details/{assessmentId}

Returns the per-pillar, per-element `RiskAssessmentDetail` rows for a single assessment. Use this to retrieve the full breakdown (entity, industry, geo, product, channel) behind an assessment summary.

### Request

```http
GET /api/internal/risk/assessment-details/{assessmentId}
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `assessmentId` | `long` | **Yes** | The `assessmentID` from a `RiskAssessment`. |

#### Example Request

```http
GET /api/internal/risk/assessment-details/1
```

### Response

**HTTP 200 OK** — `array<RiskAssessmentDetail>`

| Field | Type | Description |
|---|---|---|
| `detailID` | `long` | Auto-generated detail row ID. |
| `assessmentID` | `long` | Foreign key to the parent `RiskAssessment`. |
| `riskType` | `string` | Pillar name: `entity`, `industry`, `geo`, `product`, or `channel`. |
| `elementName` | `string` | Risk element name (e.g. `countryOfNationality`). |
| `elementValue` | `string` | Evaluated value (e.g. `DE`). |
| `riskScore` | `integer` | Score for this element. |
| `flag` | `string` | Override flag, if set. |
| `localRuleApplied` | `string` | Whether a local rule was applied (`Y`/`N`). |

#### Example Response

```json
[
  {
    "detailID": 1,
    "assessmentID": 1,
    "riskType": "geo",
    "elementName": "countryOfNationality",
    "elementValue": "DE",
    "riskScore": 12,
    "flag": null,
    "localRuleApplied": "N"
  },
  {
    "detailID": 2,
    "assessmentID": 1,
    "riskType": "entity",
    "elementName": "typeKYCLegalEntityCode",
    "elementValue": "NP4",
    "riskScore": 14,
    "flag": null,
    "localRuleApplied": "N"
  }
]
```

---

## 6. POST /api/internal/risk/batch/initiate

Creates a batch run record for the given clients and processes it end-to-end in a single step: generates the JSONL payload, zips the output, creates the control file, and uploads to SFTP. Returns the batch name on success.

### Request

```http
POST /api/internal/risk/batch/initiate
Content-Type: application/json
X-Internal-Api-Key: <key>
```

#### Request Body — `array<Client>`

See [Client Object](#client-object) in Data Dictionaries.

#### Example Request

```json
[
  {
    "clientID": 1001,
    "firstName": "Jane",
    "lastName": "Smith",
    "dateOfBirth": "1985-06-15",
    "citizenship1": "DE",
    "country": "DE",
    "occupation": "Software Engineer"
  }
]
```

### Response

**HTTP 200 OK**

```
Batch Initiated: RISK-BATCH-20260616-001
```

**HTTP 500 Internal Server Error**

```
Failed: <error detail>
```

---

## 7. POST /api/internal/risk/batch/create

Creates a batch run record and returns the `batchId` for step-by-step processing. Does not generate files or upload to SFTP. Use this when you want to control each processing step individually.

### Request

```http
POST /api/internal/risk/batch/create
Content-Type: application/json
X-Internal-Api-Key: <key>
```

#### Request Body

Same as [POST /api/internal/risk/batch/initiate](#6-post-apiinternalriskbatchinitiate).

### Response

**HTTP 200 OK** — the batch ID (opaque string).

```
RISK-BATCH-20260616-001
```

---

## 8. POST /api/internal/risk/batch/{batchId}/generate-jsonl

Generates the JSONL request file for the specified batch using the configured field mappings. The file is written to the batch working directory.

### Request

```http
POST /api/internal/risk/batch/{batchId}/generate-jsonl
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `batchId` | `string` | **Yes** | The batch ID returned by `POST /batch/create`. |

### Response

**HTTP 200 OK**

```
JSONL Generated
```

---

## 9. POST /api/internal/risk/batch/{batchId}/zip

Compresses the generated batch files (JSONL + control) into a ZIP archive.

### Request

```http
POST /api/internal/risk/batch/{batchId}/zip
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `batchId` | `string` | **Yes** | Batch ID to compress. |

### Response

**HTTP 200 OK**

```
Files Zipped
```

---

## 10. POST /api/internal/risk/batch/{batchId}/generate-control

Creates the control/manifest file for the batch submission. The control file accompanies the JSONL in the ZIP and describes the batch to the CRRE processing system.

### Request

```http
POST /api/internal/risk/batch/{batchId}/generate-control
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `batchId` | `string` | **Yes** | Batch ID for which to generate the control file. |

### Response

**HTTP 200 OK**

```
Control File Generated
```

---

## 11. POST /api/internal/risk/batch/{batchId}/upload

Uploads the compressed batch archive to the configured SFTP server. Requires the batch to have been zipped first. Behaviour depends on the `sftp.enabled` configuration flag (see [Configuration Properties](#configuration-properties)).

### Request

```http
POST /api/internal/risk/batch/{batchId}/upload
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `batchId` | `string` | **Yes** | Batch ID to upload. |

### Response

**HTTP 200 OK**

```
Uploaded (Mock/Real based on config)
```

---

## 12. GET /api/internal/risk/batch/history

Returns the full list of batch run records with their statuses.

### Request

```http
GET /api/internal/risk/batch/history
X-Internal-Api-Key: <key>
```

### Response

**HTTP 200 OK** — `array<BatchRun>`

| Field | Type | Description |
|---|---|---|
| `batchID` | `long` | Auto-generated batch record ID. |
| `batchName` | `string` | Human-readable batch name (e.g. `RISK-BATCH-20260616-001`). |
| `runStatus` | `string` | Processing status: `CREATED`, `JSONL_GENERATED`, `ZIPPED`, `UPLOADED`, `COMPLETE`, `FAILED`. |
| `notificationStatus` | `string` | Downstream notification status. |
| `feedbackCount` | `integer` | Number of feedback records received for this batch. |
| `createdAt` | `string` (ISO-8601) | When the batch was created. |
| `updatedAt` | `string` (ISO-8601) | When the batch record was last updated. |

#### Example Response

```json
[
  {
    "batchID": 1,
    "batchName": "RISK-BATCH-20260616-001",
    "runStatus": "UPLOADED",
    "notificationStatus": null,
    "feedbackCount": 0,
    "createdAt": "2026-06-16T10:00:00.000",
    "updatedAt": "2026-06-16T10:01:30.000"
  }
]
```

---

## 13. GET /api/internal/risk/batch/{batchId}/file-content

Returns the raw content of a generated batch file by type. Useful for inspecting generated outputs before or after upload.

### Request

```http
GET /api/internal/risk/batch/{batchId}/file-content?type=<type>
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `batchId` | `string` | **Yes** | Batch ID. |

#### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `type` | `string` | **Yes** | File type to retrieve: `jsonl`, `zip`, or `control`. |

#### Example Request

```http
GET /api/internal/risk/batch/RISK-BATCH-20260616-001/file-content?type=jsonl
```

### Response

**HTTP 200 OK** — plain text content of the requested file.

---

## 14. GET /api/internal/risk/batch/mapping

Returns the currently configured field mapping between source client fields and the CRRE JSONL input paths.

### Request

```http
GET /api/internal/risk/batch/mapping
X-Internal-Api-Key: <key>
```

### Response

**HTTP 200 OK** — `array<RiskMapping>`

| Field | Type | Description |
|---|---|---|
| `mappingID` | `long` | Auto-generated mapping row ID. |
| `targetPath` | `string` | JSON path in the CRRE JSONL output (e.g. `clientDetails.recordID`). |
| `sourceField` | `string` | Source field name on the `Client` object (e.g. `clientID`). |
| `defaultValue` | `string` | Static default value used when `sourceField` is absent or null. |
| `category` | `string` | Grouping category (e.g. `clientDetails`, `geoRiskType`). |

#### Example Response

```json
[
  {
    "mappingID": 1,
    "targetPath": "clientDetails.recordID",
    "sourceField": "clientID",
    "defaultValue": null,
    "category": "clientDetails"
  },
  {
    "mappingID": 2,
    "targetPath": "geoRiskType.partyAccount[0].countryOfNationality[0]",
    "sourceField": "citizenship1",
    "defaultValue": "DE",
    "category": "geoRiskType"
  }
]
```

---

## 15. POST /api/internal/risk/batch/mapping

Saves or replaces the field mapping configuration. The full list of mappings is replaced atomically.

### Request

```http
POST /api/internal/risk/batch/mapping
Content-Type: application/json
X-Internal-Api-Key: <key>
```

#### Request Body

`array<RiskMapping>` — same schema as [GET /api/internal/risk/batch/mapping](#14-get-apiinternalriskbatchmapping).

### Response

**HTTP 200 OK** — empty body.

---

## 16. POST /api/internal/risk/batch/test-generate

Generates a sample JSONL payload for a single client using the current mapping configuration. Useful for validating a mapping without creating a full batch run.

### Request

```http
POST /api/internal/risk/batch/test-generate
Content-Type: application/json
X-Internal-Api-Key: <key>
```

#### Request Body — `Client`

See [Client Object](#client-object) in Data Dictionaries.

### Response

**HTTP 200 OK** — the generated JSONL string for the supplied client.

```
{"header":{...},"clientRiskRatingRequest":[{...}]}
```

---

## 17. Data Dictionaries

### Client Object

Used as the input for batch endpoints. Fields map to CRRE inputs via the `RiskMapping` configuration.

| Field | Type | Description |
|---|---|---|
| `clientID` | `long` | Internal client identifier. Typically maps to `clientDetails.recordID`. |
| `titlePrefix` | `string` | Title prefix (e.g. `Mr`, `Ms`, `Dr`). |
| `firstName` | `string` | Given name. |
| `middleName` | `string` | Middle name. |
| `lastName` | `string` | Family name. |
| `titleSuffix` | `string` | Title suffix (e.g. `Jr`, `PhD`). |
| `citizenship1` | `string` | Primary citizenship country code (ISO 3166-1 alpha-2). |
| `citizenship2` | `string` | Secondary citizenship country code. |
| `onboardingDate` | `string` (`YYYY-MM-DD`) | Date the client was onboarded. |
| `status` | `string` | Client account status. |
| `nameAtBirth` | `string` | Name at birth / birth name. |
| `nickName` | `string` | Preferred name or nickname. |
| `gender` | `string` | Gender code. |
| `dateOfBirth` | `string` (`YYYY-MM-DD`) | Date of birth. |
| `language` | `string` | Preferred language code. |
| `occupation` | `string` | Occupation description or code. |
| `countryOfTax` | `string` | Country of tax residency. |
| `sourceOfFundsCountry` | `string` | Country of origin of funds. |
| `fatcaStatus` | `string` | FATCA classification status. |
| `crsStatus` | `string` | CRS classification status. |
| `addressLine1` | `string` | Street address. |
| `city` | `string` | City of residence. |
| `zipCode` | `string` | Postal / ZIP code. |
| `province` | `string` | Province or state. |
| `country` | `string` | Country of residence (ISO 3166-1 alpha-2). |
| `nationality` | `string` | Nationality country code. |
| `legDocType` | `string` | Legitimisation document type (e.g. `PASSPORT`). |
| `idNumber` | `string` | Document identification number. |
| `placeOfBirth` | `string` | Place of birth (city/region). |
| `cityOfBirth` | `string` | City of birth. |
| `countryOfBirth` | `string` | Country of birth (ISO 3166-1 alpha-2). |

### RiskLevel

| Value | Description |
|---|---|
| `LOW` | Low risk — standard onboarding proceeds. |
| `MEDIUM` | Medium risk — enhanced due diligence applies per policy. |
| `HIGH` | High risk — triggers `ON_HOLD` in KYC Orchestration; mandatory analyst review. |

### BatchRunStatus

| Value | Description |
|---|---|
| `CREATED` | Batch record created; no files generated yet. |
| `JSONL_GENERATED` | JSONL input file written to disk. |
| `ZIPPED` | Batch files compressed into a ZIP archive. |
| `UPLOADED` | ZIP uploaded to the SFTP server. |
| `COMPLETE` | All steps completed successfully. |
| `FAILED` | Processing failed at one or more steps. |

---

## 18. Error Responses

All error responses follow a standard format:

```json
{
  "timestamp": "2026-06-16T10:05:00.000",
  "status": 403,
  "error": "Forbidden",
  "message": "Missing or invalid internal API key"
}
```

| HTTP Status | Trigger |
|---|---|
| `403 Forbidden` | `X-Internal-Api-Key` header missing or value does not match configured key |
| `400 Bad Request` | Missing required fields in request body |
| `404 Not Found` | `recordId`, `assessmentId`, or `batchId` does not exist |
| `500 Internal Server Error` | Unexpected processing error (batch generation failure, SFTP error, etc.) |

---

## 19. Architecture & Security Notes

### Internal API Key Authentication

All endpoints under `/api/internal/**` are protected by the `X-Internal-Api-Key` header. This is a shared secret known only to internal services (e.g. KYC Orchestration). The key is configured via the `internal.api.key` property (`INTERNAL_API_KEY` env var). Any request with a missing or mismatched key is rejected with `403 Forbidden` before the request reaches any controller.

### CRRE Integration Modes

The Risk Service supports two CRRE integration modes, toggled by the `crre.mock` property:

| Mode | Property | Behaviour |
|---|---|---|
| **Mock** (default) | `crre.mock=true` | `MockRiskProvider` handles all risk calculations in-process. No network calls made. Deterministic results: `clientAdoptionCountry=CU` → `HIGH`; all others → `LOW`. |
| **Real** | `crre.mock=false` | `CrreRiskProvider` sends requests to the CRRE REST API at `crre.base-url`. Supports mTLS when `crre.mtls.enabled=true`. |

### Batch Processing Pipeline

The batch endpoints expose each stage of the pipeline individually. The `POST /batch/initiate` endpoint runs all stages sequentially in one call. The step-by-step alternative (create → generate-jsonl → zip → generate-control → upload) is intended for debugging or re-running failed stages.

```
POST /batch/create
      │
      ▼
POST /{batchId}/generate-jsonl   ← writes JSONL to batch.work.dir
      │
      ▼
POST /{batchId}/generate-control ← writes control/manifest file
      │
      ▼
POST /{batchId}/zip              ← compresses JSONL + control into ZIP
      │
      ▼
POST /{batchId}/upload           ← uploads ZIP to SFTP server
```

### SFTP

SFTP is disabled by default (`sftp.enabled=false`). When disabled, the upload step completes successfully without transmitting any file (mock mode). Set `SFTP_ENABLED=true` and provide host/credentials to activate real uploads.

### Configuration Properties

| Property | Default | Description |
|---|---|---|
| `server.port` | `8081` | HTTP listener port |
| `internal.api.key` | `dev-internal-kyc-key-change-in-prod` | Shared secret for `X-Internal-Api-Key` validation. Override via `INTERNAL_API_KEY` env var. |
| `crre.mock` | `true` | `true` = MockRiskProvider; `false` = CrreRiskProvider with real CRRE API |
| `crre.base-url` | `https://crre-api.example.com` | Base URL of the real CRRE API |
| `crre.caller-system` | `KYC-SERVICE` | System identifier sent in the CRRE request header |
| `crre.crrm-version` | `2.0` | CRRE model version |
| `crre.http-timeout-ms` | `30000` | HTTP timeout for CRRE calls (milliseconds) |
| `crre.mtls.enabled` | `false` | Enable mTLS for CRRE calls |
| `crre.mtls.key-store-path` | *(empty)* | Path to the PKCS12 keystore for client certificate |
| `crre.mtls.key-store-password` | *(empty)* | Password for the keystore |
| `crre.mtls.trust-store-path` | *(empty)* | Path to a custom truststore (leave blank for JVM default) |
| `sftp.host` | `localhost` | SFTP server hostname |
| `sftp.port` | `22` | SFTP server port |
| `sftp.user` | `user` | SFTP username |
| `sftp.enabled` | `false` | `true` = real SFTP upload; `false` = mock upload |
| `batch.work.dir` | `/tmp/risk-batch` | Local directory for generated batch files |
| `eureka.client.service-url.defaultZone` | `http://localhost:8761/eureka/` | Eureka server location |

**Production checklist:**
- [ ] Set `INTERNAL_API_KEY` to a cryptographically random string (≥32 characters) — **never commit the real key**
- [ ] Set `CRRE_MOCK=false` and configure `CRRE_BASE_URL` to the production CRRE endpoint
- [ ] Enable mTLS (`CRRE_MTLS_ENABLED=true`) and provide keystore/truststore credentials via secrets manager
- [ ] Set `SFTP_ENABLED=true` and provide real SFTP credentials
- [ ] Disable H2 console (`H2_CONSOLE=false`) and replace H2 datasource with a production RDBMS
- [ ] Ensure `BATCH_WORK_DIR` is a persistent, appropriately sized volume
