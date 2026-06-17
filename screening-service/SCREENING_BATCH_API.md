# Screening Service — Batch API Reference

**Base URL:** `http://<host>:8082`  
**Content-Type:** `application/json`  
**Service Name (Eureka):** `screening-service`

> See also: [SCREENING_REALTIME_API.md](SCREENING_REALTIME_API.md) for single-client real-time screening.

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Standard Pipeline](#2-standard-pipeline)
3. [Batch Endpoints](#3-batch-endpoints)
   - [POST /api/internal/screening/batch/create](#31-post-apiinternalscreeningbatchcreate)
   - [POST /api/internal/screening/batch/{batchId}/generate-xml](#32-post-apiinternalscreeningbatchbatchidgenerate-xml)
   - [POST /api/internal/screening/batch/{batchId}/generate-checksum](#33-post-apiinternalscreeningbatchbatchidgenerate-checksum)
   - [POST /api/internal/screening/batch/{batchId}/zip](#34-post-apiinternalscreeningbatchbatchidzip)
   - [POST /api/internal/screening/batch/{batchId}/encrypt](#35-post-apiinternalscreeningbatchbatchidencrypt)
   - [POST /api/internal/screening/batch/{batchId}/upload](#36-post-apiinternalscreeningbatchbatchidupload)
   - [POST /api/internal/screening/batch/process](#37-post-apiinternalscreeningbatchprocess)
   - [GET /api/internal/screening/batch/history](#38-get-apiinternalscreeningbatchhistory)
   - [GET /api/internal/screening/batch/{batchId}/file-content](#39-get-apiinternalscreeningbatchbatchidfile-content)
   - [GET /api/internal/screening/batch/runs/{runGroupId}](#310-get-apiinternalscreeningbatchrunsrungroupid)
   - [GET /api/internal/screening/batch/runs?fileName=](#311-get-apiinternalscreeningbatchrunsfilename)
4. [Field Mapping Configuration](#4-field-mapping-configuration)
5. [Data Dictionaries](#5-data-dictionaries)
6. [Error Responses](#6-error-responses)
7. [Batch Screening Flow](#7-batch-screening-flow)
8. [Configuration](#8-configuration)

---

## 1. Authentication

All endpoints under `/api/internal/**` require an internal API key header:

```http
X-Internal-Api-Key: <key>
```

Requests without a valid key are rejected with `403 Forbidden`. The key is configured via the `INTERNAL_API_KEY` environment variable (default dev value: `dev-internal-kyc-key-change-in-prod`).

---

## 2. Standard Pipeline

Batch screening processes large volumes of clients by generating a vendor-compatible XML file, computing a checksum, compressing, GPG-encrypting, and uploading via SFTP. Each step is an independent endpoint so the pipeline can be replayed from any stage.

```
create → generate-xml → generate-checksum → zip → encrypt → upload
                                                              ↓
                                              [vendor processes and returns response]
                                                              ↓
                                                         process
```

---

## 3. Batch Endpoints

### 3.1 POST /api/internal/screening/batch/create

Creates a new batch run record for the supplied clients without starting any processing. Returns the `batchId` to use in all subsequent step endpoints.

#### Request

```http
POST /api/internal/screening/batch/create
Content-Type: application/json
X-Internal-Api-Key: <key>
```

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `source` | `string` | `MANUAL` | Origin of the batch (e.g. `MANUAL`, `SCHEDULED`). |
| `createdBy` | `string` | `SYSTEM` | Identifier of the user or service creating the batch. |

#### Request Body

Array of client objects. Each client's fields are mapped to the screening XML using the active [field mapping configuration](#4-field-mapping-configuration).

#### Response — HTTP 200 OK

Returns the `batchId` as a plain string:

```
"47"
```

---

### 3.2 POST /api/internal/screening/batch/{batchId}/generate-xml

Generates the screening XML request file for the batch using the current active field mapping configuration. The mapping snapshot used is stored with the batch for audit purposes.

#### Request

```http
POST /api/internal/screening/batch/{batchId}/generate-xml
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `batchId` | `long` | **Yes** | Batch ID returned by `/create`. |

#### Response — HTTP 200 OK

```
"XML Generated"
```

---

### 3.3 POST /api/internal/screening/batch/{batchId}/generate-checksum

Calculates the SHA-256 checksum for the batch XML file and stores it alongside the batch record. The checksum file is required by the NRTS vendor for integrity verification.

#### Request

```http
POST /api/internal/screening/batch/{batchId}/generate-checksum
X-Internal-Api-Key: <key>
```

#### Response — HTTP 200 OK

```
"Checksum Generated"
```

---

### 3.4 POST /api/internal/screening/batch/{batchId}/zip

Compresses the XML and checksum files into a ZIP archive ready for encryption.

#### Request

```http
POST /api/internal/screening/batch/{batchId}/zip
X-Internal-Api-Key: <key>
```

#### Response — HTTP 200 OK

```
"Files Zipped"
```

---

### 3.5 POST /api/internal/screening/batch/{batchId}/encrypt

GPG-encrypts the ZIP archive using the configured public key (`batch.publicKeyPath`). The encrypted file is the artifact that is uploaded to the SFTP server.

#### Request

```http
POST /api/internal/screening/batch/{batchId}/encrypt
X-Internal-Api-Key: <key>
```

#### Response — HTTP 200 OK

```
"File Encrypted"
```

---

### 3.6 POST /api/internal/screening/batch/{batchId}/upload

Uploads the encrypted batch file to the SFTP server. The target directory is configured via `batch.sftp.upload.dir`. In `storage.mode=local`, the file is written to the local filesystem instead.

#### Request

```http
POST /api/internal/screening/batch/{batchId}/upload
X-Internal-Api-Key: <key>
```

#### Response — HTTP 200 OK

```
"File Uploaded"
```

---

### 3.7 POST /api/internal/screening/batch/process

Processes a screening response file received back from the vendor. Parses the XML response, updates batch run and result records, and triggers completion notifications.

#### Request

```http
POST /api/internal/screening/batch/process?filename=<filename>
X-Internal-Api-Key: <key>
```

#### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `filename` | `string` | **Yes** | Name of the response file to process. The file must exist in the configured download directory. |

#### Response — HTTP 200 OK

```
"Processing initiated for: <filename>"
```

---

### 3.8 GET /api/internal/screening/batch/history

Returns a list of all previous batch screening runs with their status summaries.

#### Request

```http
GET /api/internal/screening/batch/history
X-Internal-Api-Key: <key>
```

#### Response — HTTP 200 OK

Array of `BatchRun` objects:

| Field | Type | Description |
|---|---|---|
| `batchID` | `long` | Primary key of the batch run. |
| `batchName` | `string` | Auto-generated batch name (typically includes timestamp). |
| `runStatus` | `string` | Current status. See [Batch Run Status](#batch-run-status). |
| `notificationStatus` | `string` | Vendor notification acknowledgement status. |
| `feedbackCount` | `integer` | Number of feedback records received for this batch. |
| `clientCount` | `integer` | Number of clients included in the batch. |
| `createdAt` | `string` (ISO-8601) | When the batch was created. |
| `updatedAt` | `string` (ISO-8601) | When the batch was last updated. |
| `mappingSnapshotID` | `long` | ID of the field mapping snapshot used to generate the XML. |
| `runGroupId` | `string` | Correlates sub-batches from the same mass screening run. `null` for standalone batches. |

---

### 3.9 GET /api/internal/screening/batch/{batchId}/file-content

Returns the content of a generated batch file (XML, checksum, or ZIP) as plain text. Useful for debugging and audit.

#### Request

```http
GET /api/internal/screening/batch/{batchId}/file-content?type=<type>
X-Internal-Api-Key: <key>
```

#### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `type` | `string` | **Yes** | File type to retrieve: `xml`, `checksum`, or `zip`. |

#### Response — HTTP 200 OK

Returns file content as a plain string (`text/plain`).

---

### 3.10 GET /api/internal/screening/batch/runs/{runGroupId}

Returns the overall progress of a mass screening run (e.g. a 700K-client file) identified by its `runGroupId`, including the status of each constituent sub-batch.

#### Request

```http
GET /api/internal/screening/batch/runs/{runGroupId}
X-Internal-Api-Key: <key>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `runGroupId` | `string` | **Yes** | The correlation ID from the `.meta.json` file or UUID from the `.ack.json` file returned by the mass screening pipeline. |

#### Response — HTTP 200 OK

| Field | Type | Description |
|---|---|---|
| `runGroupId` | `string` | The run group correlation ID. |
| `fileName` | `string` | Original CSV filename that triggered this run. |
| `systemId` | `string` | Source system identifier. |
| `persistClients` | `boolean` | Whether clients from this run were persisted to the Viewer database. |
| `totalClientCount` | `integer` | Total number of clients in the run. |
| `totalBatches` | `integer` | Number of sub-batches the run was split into. |
| `batchesCompleted` | `integer` | Number of sub-batches that have completed. |
| `overallStatus` | `string` | `IN_PROGRESS`, `COMPLETED`, or `FAILED`. |
| `createdAt` | `string` (ISO-8601) | When the run was created. |
| `completedAt` | `string` (ISO-8601) | When the run finished. `null` if still in progress. |
| `batches` | `array` | Sub-batch list. Each entry includes `batchId`, `batchNumber`, `batchName`, `clientCount`, `status`, `createdAt`, `updatedAt`. |

#### Example Response

```json
{
  "runGroupId": "c3f9a712-1e4b-41c8-b7d0-0ab9f2231e84",
  "fileName": "DOWNSTREAM_A_clients_20260519143022.csv",
  "systemId": "EIS",
  "persistClients": false,
  "totalClientCount": 700000,
  "totalBatches": 700,
  "batchesCompleted": 693,
  "overallStatus": "IN_PROGRESS",
  "createdAt": "2026-05-19T14:30:22",
  "completedAt": null,
  "batches": [
    {
      "batchId": 101,
      "batchNumber": 1,
      "batchName": "batch_20260519_001",
      "clientCount": 1000,
      "status": "COMPLETED",
      "createdAt": "2026-05-19T14:30:25",
      "updatedAt": "2026-05-19T14:35:10"
    }
  ]
}
```

---

### 3.11 GET /api/internal/screening/batch/runs?fileName=

Looks up the run status of a mass screening run by the original CSV filename dropped on the input SFTP.

#### Request

```http
GET /api/internal/screening/batch/runs?fileName=DOWNSTREAM_A_clients_20260519143022.csv
X-Internal-Api-Key: <key>
```

#### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `fileName` | `string` | **Yes** | Exact original CSV filename (e.g. `DOWNSTREAM_A_clients_20260519143022.csv`). |

#### Response

Same schema as [GET /runs/{runGroupId}](#310-get-apiinternalscreeningbatchrunsrungroupid). Returns `404 Not Found` if no run matches the filename.

---

## 4. Field Mapping Configuration

The batch pipeline uses a dynamic mapping layer to translate client fields to screening XML fields. This allows the XML structure to be updated without code changes.

### GET /api/internal/screening/batch/mapping

Returns the current active field mapping configuration.

```http
GET /api/internal/screening/batch/mapping
X-Internal-Api-Key: <key>
```

**Response — HTTP 200 OK:** Array of `MappingConfig` objects:

| Field | Type | Description |
|---|---|---|
| `mappingID` | `long` | Primary key. |
| `targetPath` | `string` | Dot-notation path in the screening XML (e.g. `record.name`, `record.dob`). |
| `sourceField` | `string` | Field name on the incoming client object. |
| `defaultValue` | `string` | Value to use when `sourceField` is absent or null. |
| `transformation` | `string` | Optional transformation to apply (e.g. `UPPERCASE`, `DATE_FORMAT`). |

### POST /api/internal/screening/batch/mapping

Saves or replaces the entire mapping configuration. Accepts an array of `MappingConfig` objects (same schema as above, `mappingID` may be null for new entries).

```http
POST /api/internal/screening/batch/mapping
Content-Type: application/json
X-Internal-Api-Key: <key>
```

### GET /api/internal/screening/batch/{batchId}/mapping-snapshot

Returns the field mapping snapshot that was active when the specified batch was created. Useful for audit — proves exactly which mapping produced a given XML file.

### GET /api/internal/screening/batch/mapping-snapshots

Returns all versioned mapping configuration snapshots.

### POST /api/internal/screening/batch/test-generate

Generates a sample screening XML payload for a single client using the current active mapping configuration. Use this to validate mapping changes before running a full batch.

```http
POST /api/internal/screening/batch/test-generate
Content-Type: application/json
X-Internal-Api-Key: <key>
```

**Request body:** A single client object (same structure used in `/create`).

**Response:** The generated XML string (`text/plain`).

---

## 5. Data Dictionaries

### Batch Run Status

| Value | Description |
|---|---|
| `PENDING` | Batch created; no processing started. |
| `XML_GENERATED` | Screening XML file produced. |
| `CHECKSUM_GENERATED` | SHA-256 checksum calculated. |
| `ZIPPED` | XML and checksum compressed into ZIP. |
| `ENCRYPTED` | ZIP GPG-encrypted. |
| `UPLOADED` | Encrypted file sent to SFTP. |
| `COMPLETED` | Vendor response processed successfully. |
| `FAILED` | Processing failed at some pipeline stage. |

---

## 6. Error Responses

Batch step endpoints return errors as a plain string body:

```http
HTTP 500 Internal Server Error

"Failed: <message>"
```

| HTTP Status | Trigger |
|---|---|
| `403 Forbidden` | Missing or invalid `X-Internal-Api-Key` header |
| `404 Not Found` | Run group ID or filename not found (`/runs` endpoints) |
| `500 Internal Server Error` | File I/O failure, SFTP error, GPG encryption failure, or XML generation error |

---

## 7. Batch Screening Flow

```
Caller                        Screening Service         SFTP / Vendor
  │                                   │                      │
  │  POST /batch/create               │                      │
  │  [{clients...}]                   │                      │
  │──────────────────────────────────►│                      │
  │◄─ "47"  (batchId) ────────────────│                      │
  │                                   │                      │
  │  POST /batch/47/generate-xml      │                      │
  │──────────────────────────────────►│── map fields         │
  │◄─ "XML Generated" ────────────────│── write .xml file    │
  │                                   │                      │
  │  POST /batch/47/generate-checksum │                      │
  │──────────────────────────────────►│── SHA-256 checksum   │
  │◄─ "Checksum Generated" ───────────│                      │
  │                                   │                      │
  │  POST /batch/47/zip               │                      │
  │──────────────────────────────────►│── compress .zip      │
  │◄─ "Files Zipped" ─────────────────│                      │
  │                                   │                      │
  │  POST /batch/47/encrypt           │                      │
  │──────────────────────────────────►│── GPG encrypt        │
  │◄─ "File Encrypted" ───────────────│                      │
  │                                   │                      │
  │  POST /batch/47/upload            │                      │
  │──────────────────────────────────►│── SFTP put ─────────►│
  │◄─ "File Uploaded" ────────────────│                      │
  │                                   │                      │
  │  [vendor processes and returns response file]            │
  │                                   │                      │
  │  POST /batch/process              │◄── response file ────│
  │  ?filename=response_47.xml        │                      │
  │──────────────────────────────────►│── parse & update DB  │
  │◄─ "Processing initiated..." ──────│                      │
  │                                   │                      │
  │  GET /batch/history               │                      │
  │──────────────────────────────────►│                      │
  │◄─ [{batchId:47, status:COMPLETED}]│                      │
```

---

## 8. Configuration

### Storage Mode

Batch file output is controlled by `storage.mode`:

| `storage.mode` | Provider | Behaviour |
|---|---|---|
| `local` (default) | `LocalFileStorageService` | Files written to `storage.local.base-dir` (default: `/tmp/screening-local`). No SFTP required. |
| `sftp` | `SftpFileStorageService` | Files uploaded to the configured SFTP server. |

### Configuration Properties

| Property | Env Var | Default | Description |
|---|---|---|---|
| `server.port` | `PORT` | `8082` | HTTP listener port |
| `internal.api.key` | `INTERNAL_API_KEY` | `dev-internal-kyc-key-change-in-prod` | Shared secret for `X-Internal-Api-Key` auth |
| `storage.mode` | `STORAGE_MODE` | `local` | `local` or `sftp` |
| `storage.local.base-dir` | `STORAGE_LOCAL_BASE_DIR` | `/tmp/screening-local` | Local file storage root (when `storage.mode=local`) |
| `sftp.host` | `SFTP_HOST` | `localhost` | SFTP server hostname |
| `sftp.port` | `SFTP_PORT` | `22` | SFTP port |
| `sftp.user` | `SFTP_USER` | `user` | SFTP username |
| `sftp.password` | `SFTP_PASSWORD` | `password` | SFTP password |
| `batch.size` | `BATCH_SIZE` | `1000` | Clients per sub-batch in mass screening runs |
| `batch.work.dir` | `BATCH_WORK_DIR` | `batch-work` | Working directory for temporary batch files |
| `batch.publicKeyPath` | `GPG_PUBLIC_KEY_PATH` | `batch-work/pubring.gpg` | GPG public key for batch file encryption |
| `batch.privateKeyPath` | `GPG_PRIVATE_KEY_PATH` | `batch-work/secring.gpg` | GPG private key |
| `batch.passphrase` | `GPG_PASSPHRASE` | `password` | GPG key passphrase |
| `batch.keep-temp-files` | `BATCH_KEEP_TEMP_FILES` | `true` | Retain XML/checksum/ZIP files after upload |
| `batch.persist-clients` | `BATCH_PERSIST_CLIENTS` | `false` | Write mass screening clients to the Viewer database |
| `eureka.client.serviceUrl.defaultZone` | `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka server location |

**Production checklist:**
- [ ] Set `INTERNAL_API_KEY` to a secret value shared with all calling services
- [ ] Set `STORAGE_MODE=sftp` and configure SFTP credentials
- [ ] Replace the dev GPG keys in `batch.publicKeyPath` / `batch.privateKeyPath` with production keys
- [ ] Disable H2 console (`spring.h2.console.enabled=false`)
- [ ] Replace H2 datasource with a production RDBMS
