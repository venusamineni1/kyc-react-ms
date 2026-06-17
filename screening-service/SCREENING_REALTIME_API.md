# Screening Service — Real-Time API Reference

**Base URL:** `http://<host>:8082`  
**Content-Type:** `application/json`  
**Service Name (Eureka):** `screening-service`

> See also: [SCREENING_BATCH_API.md](SCREENING_BATCH_API.md) for the batch pipeline.

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Endpoints](#2-endpoints)
   - [POST /api/internal/screening/initiate](#21-post-apiinternalscreeninginitiate)
   - [GET /api/internal/screening/status/{processId}](#22-get-apiinternalscreeningstatusprocessid)
   - [GET /api/internal/screening/details/{reqId}](#23-get-apiinternalscreeningdetailsreqid)
   - [GET /api/internal/screening/document/{documentId}](#24-get-apiinternalscreeningdocumentdocumentid)
   - [GET /api/internal/screening/history/{clientId}](#25-get-apiinternalscreeninghistoryclientid)
3. [Data Dictionaries](#3-data-dictionaries)
4. [NRTS XML Format](#4-nrts-xml-format)
5. [Error Responses](#5-error-responses)
6. [Screening Flow](#6-screening-flow)
7. [Configuration](#7-configuration)

---

## 1. Authentication

All endpoints under `/api/internal/**` require an internal API key header:

```http
X-Internal-Api-Key: <key>
```

Requests without a valid key are rejected with `403 Forbidden`. The key is configured via the `INTERNAL_API_KEY` environment variable (default dev value: `dev-internal-kyc-key-change-in-prod`). The same key must be set on all services that call the Screening Service (e.g. the Viewer and KYC Orchestration).

---

## 2. Endpoints

Real-time screening submits a single client to the NRTS (NLS Real-Time Screening) service and returns sanctions/PEP/ADM/INT hit context synchronously. When `nrts.mock=true` (default in dev), a mock provider is used instead of the live NRTS system.

### 2.1 POST /api/internal/screening/initiate

Submits a single client to NRTS. If alerts are found (NRTS returns HTTP 202), the service waits `statusCheckDelayMs` milliseconds then immediately calls NRTS `get_status` once to retrieve initial alert context. All four screening contexts (PEP, ADM, INT, SAN) are persisted to the screening log.

#### Request

```http
POST /api/internal/screening/initiate
Content-Type: application/json
X-Internal-Api-Key: <key>
```

#### Request Body

| Field | Type | Required | Description |
|---|---|---|---|
| `clientId` | `long` | No | Internal client ID (≤100 chars). Stored on the screening log and included in `<p:ClientId>`. |
| `firstName` | `string` | **Yes** | Client's first name. Combined with `lastName` as `LASTNAME,FIRSTNAME` in `<p:Name>`. |
| `lastName` | `string` | **Yes** | Client's last name. |
| `dateOfBirth` | `string` | No | Date of birth — `yyyy` or `yyyy-MM-DD`. Maps to `<p:DOB>`. Dashes are stripped for checksum computation. |
| `gender` | `string` | No | `"M"` or `"F"`. Maps to `<p:G>`. Included in the checksum computation — must be provided if the person has a gender on record; omitting it when gender is known will produce a wrong checksum. |
| `citizenship` | `string` | No | ISO 3166-1 alpha-2 country code. Maps to `<p:Cntr>`. |
| `nationality` | `string` | No | ISO 3166-1 alpha-2. Maps to `<p:Nat>`. Only for individuals. If omitted, the element is not sent — NRTS will not infer it from `citizenship`. |
| `countryOfResidence` | `string` | No | ISO 3166-1 alpha-2. Maps to `<p:CntrRes>`. If omitted, the element is not sent. |
| `idType` | `string` | No | Identity document type (alphanumeric). Maps to `<p:IdType>`. |
| `idNumber` | `string` | No | Identity document number (alphanumeric). Maps to `<p:IdNr>`. |
| `riskRating` | `string` | No | Client risk rating: `"H"`, `"M"`, or `"L"`. Maps to `<p:Risk>`. |
| `comment` | `string` | No | Free-text comment (alphanumeric). Maps to `<p:Comment>`. |
| `province` | `string` | No | Province or state (alphanumeric). Maps to `<p:Prov>`. |
| `statusCheckDelayMs` | `long` | No | Milliseconds to wait after NRTS submit before calling `get_status`. `0` = immediate. Overrides the global `nrts.status-check-delay-ms` setting for this request. |

#### Example Request

```json
{
  "clientId": 42,
  "firstName": "Hans",
  "lastName": "Mueller",
  "dateOfBirth": "1975-08-20",
  "gender": "M",
  "citizenship": "DE",
  "nationality": "DE",
  "countryOfResidence": "DE",
  "idType": "PASSPORT",
  "idNumber": "C01X00T47",
  "riskRating": "L",
  "comment": "KYC onboarding",
  "province": null,
  "statusCheckDelayMs": 0
}
```

#### Response — HTTP 200 OK

| Field | Type | Description |
|---|---|---|
| `result` | `string` | `"Hot"` — one or more alert contexts matched. `"No-Hit"` — no matches found. |
| `processId` | `long` | NRTS numeric process ID. Use this to call `GET /status/{processId}` for follow-up polling. `null` on `No-Hit`. |
| `reqId` | `long` | NRTS request ID for this client. Required by `GET /details/{reqId}` once the investigation is finalized. `null` on `No-Hit`. |
| `alertContexts` | `array<string>` | Alert context types that matched (e.g. `["PEP", "INT"]`). Empty array on `No-Hit`. See [Context Types](#context-types). |

#### Example Response — Hit

```json
{
  "result": "Hot",
  "processId": 908171,
  "reqId": 1042,
  "alertContexts": ["PEP", "INT"]
}
```

#### Example Response — No-Hit

```json
{
  "result": "No-Hit",
  "processId": null,
  "reqId": null,
  "alertContexts": []
}
```

#### Side Effects

- A `ScreeningLog` record is persisted with `overallStatus = IN_PROGRESS` (Hot) or `COMPLETED` (No-Hit).
- One `ScreeningResult` row is persisted per context type (PEP, ADM, INT, SAN). Matched contexts are marked `HIT`; others are marked `NO_HIT`.

---

### 2.2 GET /api/internal/screening/status/{processId}

Polls NRTS `get_status` for the given `processId` to retrieve the current investigation status per context. The **caller is responsible** for scheduling repeated invocations. Stop polling when `finalized=true`.

#### Request

```http
GET /api/internal/screening/status/{processId}
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `processId` | `long` | **Yes** | The NRTS `processId` returned by `/initiate`. |

#### Example Request

```http
GET /api/internal/screening/status/908171
```

#### Response — HTTP 200 OK

| Field | Type | Description |
|---|---|---|
| `requestId` | `string` | The `processId` as a string (mirrors the NRTS external request ID). |
| `overallStatus` | `string` | `"In Progress"`, `"With SIU"`, or `"Finished"`. |
| `finalized` | `boolean` | `true` when the investigation is complete. Once `true`, stop polling and call `GET /details/{reqId}`. |
| `reqId` | `long` | NRTS `reqId` for the client — pass to `/details/{reqId}`. May be `null` until the investigation advances. |
| `results` | `array<ContextResult>` | Per-context status breakdown. |

**ContextResult object:**

| Field | Type | Description |
|---|---|---|
| `contextType` | `string` | `PEP`, `ADM`, `INT`, or `SAN`. |
| `status` | `string` | `HIT`, `NO_HIT`, or `IN_PROGRESS`. |
| `alertMessage` | `string` | Human-readable status message from NRTS. |

#### Example Response

```json
{
  "requestId": "908171",
  "overallStatus": "Finished",
  "finalized": true,
  "reqId": 1042,
  "results": [
    { "contextType": "PEP", "status": "HIT",     "alertMessage": "Under investigation" },
    { "contextType": "INT", "status": "HIT",     "alertMessage": "Under investigation" },
    { "contextType": "ADM", "status": "NO_HIT",  "alertMessage": null },
    { "contextType": "SAN", "status": "NO_HIT",  "alertMessage": null }
  ]
}
```

#### Side Effects

When `finalized=true`, the corresponding `ScreeningLog` record is updated to `overallStatus = COMPLETED` and the NRTS `reqId` is stored on all result rows for the log.

#### Polling Guidance

Recommended interval: **every 6 hours**. NRTS investigations typically resolve within 24–72 hours depending on context type and manual review load.

---

### 2.3 GET /api/internal/screening/details/{reqId}

Calls NRTS `get_final_request_details` to retrieve the full alert history, operator decisions, and Filenet document references for a finalized client. Returns `409 Conflict` if the investigation is still in progress (NRTS returns `412 Precondition Failed`).

#### Request

```http
GET /api/internal/screening/details/{reqId}
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `reqId` | `long` | **Yes** | The NRTS `reqId` returned by `/status/{processId}`. Only call this after `finalized=true`. |

#### Example Request

```http
GET /api/internal/screening/details/1042
```

#### Response — HTTP 200 OK

| Field | Type | Description |
|---|---|---|
| `requestId` | `long` | The NRTS `reqId`. |
| `status` | `string` | `"FINISHED"`. |
| `alerts` | `array<AlertEntry>` | One entry per alert raised by NRTS. |

**AlertEntry object:**

| Field | Type | Description |
|---|---|---|
| `alertId` | `string` | NRTS alert identifier. The context type (PEP, INT, etc.) is derived from this ID. |
| `context` | `string` | Context type derived from `alertId` (e.g. `PEP`, `INT`). |
| `alertStatus` | `string` | Final alert status (e.g. `"False Positive"`, `"Confirmed Match"`). |
| `lastDecisionDate` | `string` | ISO-8601 date of the last operator decision. |
| `lastOperator` | `string` | ID of the last operator who acted on the alert. |
| `lastComments` | `string` | Final operator comments. |
| `hits` | `array<HitEntry>` | Matched entities. `null` for INT (internal) alerts. |
| `decisionHistory` | `array<DecisionEntry>` | Full chronological decision audit trail. |
| `alertDocuments` | `array<DocumentRef>` | Attachments uploaded during the investigation. Use `filenetId` with `GET /document/{documentId}`. |

**HitEntry object:**

| Field | Type | Description |
|---|---|---|
| `country` | `string` | Country associated with the hit entry. |
| `city` | `string` | City associated with the hit entry. |
| `name` | `string` | Name of the matched entity. |
| `origin` | `string` | Origin of the entry (e.g. list source). |
| `keywords` | `string` | Keywords from the matched list entry. |
| `type` | `string` | Entity type: `I` = Individual, `C` = Company, `O` = Other. |

**DecisionEntry object:**

| Field | Type | Description |
|---|---|---|
| `date` | `string` | ISO-8601 decision date. |
| `operator` | `string` | Operator who made the decision. |
| `state` | `string` | Decision state at this point in time. |
| `comments` | `string` | Operator comments at this decision step. |
| `document` | `DocumentRef` | Attached document, if any. `null` if no attachment for this step. |

**DocumentRef object:**

| Field | Type | Description |
|---|---|---|
| `filenetId` | `string` | Filenet document ID. Pass to `GET /document/{filenetId}` to download. |
| `comments` | `string` | Document description or comments. |
| `operator` | `string` | Operator who uploaded the document. |

#### Example Response

```json
{
  "requestId": 1042,
  "status": "FINISHED",
  "alerts": [
    {
      "alertId": "PEP-20260411-001",
      "context": "PEP",
      "alertStatus": "False Positive",
      "lastDecisionDate": "2026-04-13T14:30:00",
      "lastOperator": "analyst_1",
      "lastComments": "Name similarity only — different nationality",
      "hits": [
        {
          "country": "DE",
          "city": "Berlin",
          "name": "Mueller, Hans",
          "origin": "EU_PEP_LIST",
          "keywords": "politician",
          "type": "I"
        }
      ],
      "decisionHistory": [
        {
          "date": "2026-04-12T09:00:00",
          "operator": "analyst_1",
          "state": "Under Investigation",
          "comments": "Reviewing nationality documentation",
          "document": null
        },
        {
          "date": "2026-04-13T14:30:00",
          "operator": "analyst_1",
          "state": "False Positive",
          "comments": "Name similarity only — different nationality",
          "document": {
            "filenetId": "FN-20260413-88221",
            "comments": "Nationality document",
            "operator": "analyst_1"
          }
        }
      ],
      "alertDocuments": [
        {
          "filenetId": "FN-20260413-88221",
          "comments": "Nationality document",
          "operator": "analyst_1"
        }
      ]
    }
  ]
}
```

#### Error: Investigation Not Finalized

```http
HTTP 409 Conflict

"Investigation not yet finalized for reqId: 1042"
```

---

### 2.4 GET /api/internal/screening/document/{documentId}

Downloads an attachment from Filenet via NRTS. `documentId` is the `filenetId` found in alert details responses. Returns the raw binary file with the original `Content-Type` and `Content-Disposition` headers from NRTS.

#### Request

```http
GET /api/internal/screening/document/{documentId}
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `documentId` | `string` | **Yes** | Filenet document ID from an `AlertEntry.alertDocuments[].filenetId` or `DecisionEntry.document.filenetId`. |

#### Response

Returns the raw document bytes. `Content-Type` and `Content-Disposition` are proxied directly from NRTS.

---

### 2.5 GET /api/internal/screening/history/{clientId}

Returns all past screening logs for the given internal client ID in reverse-chronological order.

#### Request

```http
GET /api/internal/screening/history/{clientId}
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `clientId` | `long` | **Yes** | Internal client ID (matches `Clients.ClientID` in the Viewer database). |

#### Response — HTTP 200 OK

Returns an array of `ScreeningLog` objects:

| Field | Type | Description |
|---|---|---|
| `logID` | `long` | Primary key of the screening log entry. |
| `clientID` | `long` | Internal client ID. |
| `requestPayload` | `string` (JSON) | Serialised `ScreeningInternalRequest` sent to the provider. |
| `responsePayload` | `string` (JSON) | Provider response, if stored. |
| `overallStatus` | `string` | `IN_PROGRESS` or `COMPLETED`. |
| `externalRequestID` | `string` | NRTS `processId` as a string, or `NO_HIT_<timestamp>` for clean passes. |
| `createdAt` | `string` (ISO-8601) | When this screening was initiated. |
| `nrtsProcessId` | `long` | Numeric NRTS `processId` for status polling. `null` on No-Hit. |

---

## 3. Data Dictionaries

### Context Types

| Value | Meaning |
|---|---|
| `PEP` | Politically Exposed Person |
| `ADM` | Adverse Media |
| `INT` | Internal watchlist |
| `SAN` | Sanctions list |

### Screening Result Status

| Value | Description |
|---|---|
| `HIT` | Alert raised for this context type. |
| `NO_HIT` | No match found for this context type. |
| `IN_PROGRESS` | Investigation ongoing for this context type. |

### Overall Screening Log Status

| Value | Description |
|---|---|
| `IN_PROGRESS` | NRTS investigation is still open. Keep polling `/status/{processId}`. |
| `COMPLETED` | NRTS investigation is finalized (`finalized=true` was observed). |

### HitEntry Type Codes

| Value | Meaning |
|---|---|
| `I` | Individual |
| `C` | Company |
| `O` | Other |

---

## 4. NRTS XML Format

The screening service generates XML conforming to the NLS/NRTS schema. One record is submitted per real-time request.

### XML Example

All optional elements are omitted when the corresponding request field is null or blank.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Request xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.db.com/NLS_NRTS_Definition"
         xmlns:r="http://www.db.com/NLS_NRTS_Request"
         xmlns:p="http://www.db.com/NLS_NRTS_RequestInfo">
  <r:Meta>
    <p:Version>1.0</p:Version>
    <p:SrcId>2234</p:SrcId>
    <p:ChkSum>d891a8...</p:ChkSum>   <!-- SHA-256 checksum — see below -->
    <p:NoR>1</p:NoR>
  </r:Meta>
  <r:Recs>
    <r:Rec>
      <p:ClientId>42</p:ClientId>    <!-- optional -->
      <p:Type>I</p:Type>             <!-- always "I" (Individual) -->
      <p:Name>Mueller,Hans</p:Name>  <!-- LASTNAME,FIRSTNAME -->
      <p:DOB>1975-08-20</p:DOB>      <!-- optional -->
      <p:G>M</p:G>                   <!-- optional: M or F -->
      <p:IdType>PASSPORT</p:IdType>  <!-- optional -->
      <p:IdNr>C01X00T47</p:IdNr>     <!-- optional -->
      <p:Risk>L</p:Risk>             <!-- optional: H, M, or L -->
      <p:Comment>KYC onboarding</p:Comment> <!-- optional -->
      <p:Cntr>DE</p:Cntr>            <!-- optional: ISO alpha-2 citizenship -->
      <p:Nat>DE</p:Nat>              <!-- optional: ISO alpha-2 nationality -->
      <p:CntrRes>DE</p:CntrRes>      <!-- optional: ISO alpha-2 country of residence -->
      <p:Prov>Bavaria</p:Prov>       <!-- optional: province -->
    </r:Rec>
  </r:Recs>
</Request>
```

### Checksum Computation

The `<p:ChkSum>` value is SHA-256 computed over the concatenation of (in order; null/blank fields are omitted entirely):

```
SrcId + NoR + Type + Name + DOB(yyyyMMdd) + Gender + Country + Nationality + CountryResidence
```

Example from spec: `22341IDoe,John19750504MUKUKUK`
→ `2234` (SrcId) + `1` (NoR) + `I` (Type) + `Doe,John` (Name) + `19750504` (DOB) + `M` (Gender) + `UK` (Country) + `UK` (Nationality) + `UK` (CountryResidence)

> **Important:** `gender` is part of the checksum string. If you provide a gender value, it **must** be included in the request — omitting a known gender will cause a checksum mismatch and NRTS will reject the submission with `422 Unprocessable Entity`.

---

## 5. Error Responses

All real-time endpoints return errors as a plain string body:

```http
HTTP 500 Internal Server Error

"Error initiating screening: <message>"
```

| HTTP Status | Trigger |
|---|---|
| `403 Forbidden` | Missing or invalid `X-Internal-Api-Key` header |
| `409 Conflict` | `GET /details/{reqId}` called before NRTS investigation is finalized (NRTS 412) |
| `500 Internal Server Error` | NRTS communication failure, serialisation error, or unexpected exception |

---

## 6. Screening Flow

```
Caller                        Screening Service              NRTS
  │                                   │                        │
  │  POST /initiate                   │                        │
  │  {clientId, firstName, ...}       │                        │
  │──────────────────────────────────►│                        │
  │                                   │── POST /nrts/submit ──►│
  │                                   │◄─ 202 Accepted ────────│  (alerts found)
  │                                   │   (wait statusCheckDelayMs)
  │                                   │── GET /nrts/get_status ►│
  │                                   │◄─ {contexts: PEP, INT} ─│
  │◄─ 200 {result:"Hot", processId} ──│                        │
  │                                   │                        │
  │  [poll every 6 hours]             │                        │
  │  GET /status/908171               │                        │
  │──────────────────────────────────►│                        │
  │                                   │── GET /nrts/get_status ►│
  │                                   │◄─ {status: Finished} ───│
  │◄─ 200 {finalized: true, reqId} ───│                        │
  │                                   │                        │
  │  GET /details/1042                │                        │
  │──────────────────────────────────►│                        │
  │                                   │── GET /nrts/get_final  ►│
  │                                   │◄─ {alerts, decisions} ──│
  │◄─ 200 {alerts: [...]} ────────────│                        │
```

---

## 7. Configuration

The service supports two screening providers, selected via the `nrts.mock` property:

| `nrts.mock` | Provider | Behaviour |
|---|---|---|
| `true` (default) | `MockScreeningProvider` | Returns deterministic mock responses. No NRTS connectivity required. |
| `false` | `NrtsScreeningProvider` | Calls the live NRTS HTTP API. Requires `nrts.base-url`, `nrts.username`, `nrts.password`. |

| Property | Env Var | Default | Description |
|---|---|---|---|
| `server.port` | `PORT` | `8082` | HTTP listener port |
| `internal.api.key` | `INTERNAL_API_KEY` | `dev-internal-kyc-key-change-in-prod` | Shared secret for `X-Internal-Api-Key` auth |
| `nrts.base-url` | `NRTS_BASE_URL` | `http://localhost:9090` | NRTS HTTP API base URL |
| `nrts.src-id` | `NRTS_SRC_ID` | `2234` | Source system ID included in all NRTS XML requests |
| `nrts.username` | `NRTS_USERNAME` | `nrts-service-account` | NRTS HTTP Basic auth username |
| `nrts.password` | `NRTS_PASSWORD` | `changeme` | NRTS HTTP Basic auth password |
| `nrts.status-check-delay-ms` | `NRTS_STATUS_CHECK_DELAY_MS` | `0` | Global delay between NRTS submit and get_status |
| `nrts.http-timeout-ms` | `NRTS_HTTP_TIMEOUT_MS` | `30000` | HTTP timeout for NRTS calls (ms) |
| `nrts.mock` | `NRTS_MOCK` | `true` | `true` = mock provider; `false` = live NRTS |
| `eureka.client.serviceUrl.defaultZone` | `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka server location |

**Production checklist:**
- [ ] Set `INTERNAL_API_KEY` to a secret value shared with all calling services
- [ ] Set `NRTS_MOCK=false` and configure real `NRTS_BASE_URL`, `NRTS_USERNAME`, `NRTS_PASSWORD`
- [ ] Disable H2 console (`spring.h2.console.enabled=false`)
- [ ] Replace H2 datasource with a production RDBMS
