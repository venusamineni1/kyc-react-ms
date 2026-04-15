# **[DRAFT] KYC Orchestration (EIS & PPR) technical** **requirements**

## 1. Purpose & Scope

This document specifies the complete requirements for the KYC Orchestration layer as part of the New Client Adaptation (NCA) onboarding process at
DWS.


KYC Orchestration acts as the central coordinator between EIS Gateway or InvestiFi Gateway (the client-facing entry point) and three downstream
systems: KYC - NCA (case management), NLS/NRTS (real-time screening), and CRRE (risk rating).


**This document covers:**


Functional requirements — what the system must do
Integration requirements — how it interfaces with each connected system
Non-functional requirements — performance, reliability, and security expectations
Constraints and assumptions
Open items requiring clarification

## 2. Stakeholders & Roles













|Stakeholder|Role|Interest in KYC Orchestration|Contacts|
|---|---|---|---|
|EIS Gateway|Primary consumer|Calls KYC Orchestration to initiate screening; receives<br>screening results, risk rating, and final KYC feedback||
|InvestiFi|Primary consumer|Calls KYC Orchestration to initiate screening; receives<br>screening results, risk rating, and final KYC feedback||
|NLS / NRTS Team (DB)|Downstream<br>provider|Provides real-time screening API and sends final feedback files||
|CRRE Team (DB)|Downstream<br>provider|Provides real-time risk rating API||
|CDD / Coverage Teams|End users|Review NCA cases in KYC - NCA surfaced by KYC Orchestration||
|KYC Platform Team<br>(DWS)|Owner / developer|Designs, builds, and operates the KYC Orchestration service & KYC - NCA||
|AFC Team|Governance / End Users|Ensures process meets regulatory obligations||

## 3. System Context


3.1 Position in the Architecture


KYC Orchestration sits between the EIS Gateway/InvestiFi and the DWS/DB back-end systems. It has no direct user interface. All interactions are APIdriven.

|Direction|Direction|Protocol|Nature|
|---|---|---|---|
|Inbound|EIS Gateway|REST/HTTPS|Screening initiation, IDV confirmation<br>callbacks|
|Inbound|InvestiFi|REST/HTTPS|Screening initiation, IDV confirmation<br>callbacks|
|Outbound|KYC - NCA|REST/HTTPS|Client record ingestion; NCA case create /<br>update / query|
|Outbound|NLS / NRTS|REST/HTTPS|Real-time name screening (sub-second)|
|Outbound|CRRE|REST/HTTPS|Real-time risk rating (sub-second)|
|Inbound|NLS (via file feedback)|File / event|Confirmed alerts and NAR false-positive<br>notifications|
|Outbound|EIS Gateway|REST/HTTPS|Screening + risk rating results; periodic KYC<br>feedback|
|Outbound|InvestiFi Gateway|REST/HTTPS|Screening + risk rating results; periodic KYC<br>feedback|



3.2 Key Identifier


The UniqueClientID is the primary key used by KYC Orchestration across all downstream calls (KYC - NCA ingestion, KYC-NCA case API, CRRE, NLS
feedback routing, KYC Tracking DB). Its source must be confirmed — see Open Item _OI-01_ .

## 4. Functional Requirements


Requirements are categorised by capability group. Each requirement carries a priority:


MUST — mandatory for go-live


SHOULD — strongly recommended; deferral needs explicit sign-off


MAY — optional enhancement


4.1 Screening Orchestration


Covers Steps 1–4 of the NCA flow.














|ID|Priority|Requirement|Source<br>Step|
|---|---|---|---|
|KYC<br>-F-<br>01|MUST|KYC Orchestration MUST expose an API endpoint to receive a new client screening request from EIS Gateway / InvestiFi Gateway.<br>The request MUST include the UniqueClientID and all mandatory client details required for downstream screening.|Step 1|
|KYC<br>-F-<br>02|MUST|Upon receiving a screening request, KYC Orchestration MUST simultaneously invoke two downstream calls in parallel: (a) the KYC -<br>NCA Data Ingestion API to create the client record, and (b) the NLS Real-Time Screening (NRTS) API to perform name screening.|Step 2|
|KYC<br>-F-<br>03|MUST|KYC Orchestration MUST pass all available client details to the KYC - NCA ingestion call (Step 2a).|Step 2a|
|KYC<br>-F-<br>04|MUST|KYC Orchestration MUST pass the UniqueClientID and required client attributes to NLS/NRTS for screening (Step 2b).|Step 2b|
|KYC<br>-F-<br>05|MUST|KYC Orchestration MUST accept the NRTS screening result in real time (sub-second). The result MUST include a binary Hit/No-Hit<br>indicator and, in the case of a Hit, the context type (e.g., PEP, ADM).|Step 2b|
|KYC<br>-F-<br>06|MUST|After receiving the screening result, KYC Orchestration MUST invoke the CRRE API to obtain a risk rating. The request MUST include<br>both client data and the screening result (PEP).|Step 3|


|KYC<br>-F-<br>07|MUST|KYC Orchestration MUST accept the CRRE risk rating response in real time and consolidate it with the screening result.|Step 3|
|---|---|---|---|
|KYC<br>-F-<br>08|MUST|KYC Orchestration MUST return the consolidated screening result and risk rating to EIS/InvestiFi Gateway after both downstream calls<br>complete.|Step 4|



4.2 Case Creation & Tracking


Covers Step 5 of the NCA flow — triggered when screening returns a Hit OR risk rating is High.










|ID|Priority|Requirement|Source<br>Step|
|---|---|---|---|
|KYC-<br>F-10|MUST|KYC Orchestration MUST detect a screening Hit or High risk rating result and trigger the case creation sub-flow automatically.|Step 5|
|KYC-<br>F-11|MUST|KYC Orchestration MUST create a tracking record in the KYC Orchestration database with the UniqueClientID and an initial status of<br>Pending whenever a Hit or High risk is detected.|Step 5b|
|KYC-<br>F-12|MUST|KYC Orchestration MUST invoke the KYC - NCA Case API to create an NCA case. The case MUST be created in New state and<br>MUST include initial screening (unconfirmed hit) and risk rating details, keyed by UniqueClientID.|Step 5c|
|KYC-<br>F-13|MUST|Cases created in New state MUST NOT be presented to Business Operations teams for review until explicitly transitioned to Ready for<br>Review.|Step 5c|



4.3 NLS Final Feedback Handling


Covers Steps 6 & 7 — NLS sends the final feedback file after screening confirmation.








|ID|Priority|Requirement|Source<br>Step|
|---|---|---|---|
|KYC-<br>F-15|MUST|KYC Orchestration MUST poll NLS periodically until final feedback is received and route confirmed alert information to the<br>corresponding NCA case, identified by UniqueClientID.|Step 6|
|KYC-<br>F-16|MUST|KYCOrchestration MUST retrieve documents attached confirmed Alerts and upload to the corresponding NCA case, identified by<br>UniqueClientID.||
|KYC-<br>F-17|MUST|KYC - NCA MUST handle NAR (No Action Required) alerts from NLS for false-positive initial screening Hits and update the NCA case<br>accordingly.|Step 7|



4.4 IDV Confirmation & Case Status Update


Covers Steps 8 & 9 — EIS Gateway/InvestiFi Gateway notifies IDV completion, triggering a case status transition.








|ID|Priority|Requirement|Source<br>Step|
|---|---|---|---|
|KYC<br>-F-<br>18|MUST|KYC Orchestration MUST expose a callback endpoint to receive IDV completion confirmation from EIS/InvestiFi Gateway, carrying the<br>UniqueClientID and IDV reference details.|Step 8|
|KYC<br>-F-<br>19|MUST|Upon receiving a valid IDV completion confirmation, KYC Orchestration MUST invoke the KYC - NCA Case API to transition the NCA<br>case status from New to Ready for Review, using UniqueClientID as the key. If the IDV is rejected, transition the NCA cast status to<br>cancelled|Step 9|
|KYC<br>-F-<br>20|MUST|KYC Orchestration MUST reject IDV confirmation callbacks for cases that are not in New state and return an appropriate error to the<br>caller.|Step 9|



4.5 Case finalization & KYC Feedback to EIS/InvestiFi Gateway


Covers Step 11 — KYC Orchestration polls KYC - NCA and delivers final decisions to EIS/InvestFi Gateway.


|ID|Priority|Requirement|Source<br>Step|
|---|---|---|---|
|KYC-<br>F-21|MUST|KYC NCA MUST invoke KYC Orchestration to update the status of all cases currently in Pending state in the KYC Tracking DB, using<br>the KYC - NCA Case API with UniqueClientID.|Step 11|
|KYC-<br>F-23|MUST|For each case found to be in a finalized state (Proceed or Reject) in KYC - NCA, KYC Orchestration MUST send the final KYC<br>decision feedback to EIS /InvestiFy Gateway.|Step 11|
|KYC-<br>F-24|MUST|KYC Orchestration MUST update the corresponding tracking record in the KYC Tracking DB to Completed after successfully<br>delivering feedback to EIS/InvestiFi Gateway.|Step 11|
|KYC-<br>F-25|SHOULD|KYC Orchestration SHOULD guarantee at-least-once delivery of KYC feedback to EIS/InvestiFi Gateway, with idempotency handling<br>to prevent duplicate feedback for the same case.|Step 11|
|KYC-<br>F-26|MAY|KYC Orchestration MAY expose a manual feedback trigger endpoint to allow operational teams to force feedback delivery outside of<br>the scheduled polling cycle.|Step 11|


## 5. Integration Requirements

The following requirements govern how KYC Orchestration interfaces with each connected system.


5.1 Integration with EIS Gateway






|ID|Priority|Requirement|
|---|---|---|
|KYC-I-<br>01|MUST|KYC Orchestration MUST expose all inbound APIs over HTTPS with TLS 1.2 or higher.|
|KYC-I-<br>02|MUST|KYC Orchestration MUST authenticate all inbound calls from EIS/InvestiFi Gateway using OAuth 2.0 Client Credentials (Keycloak).|
|KYC-I-<br>03|MUST|KYC Orchestration MUST accept the UniqueClientID as provided by EIS/InvestiFi Gateway and use it as the primary correlation<br>key across all downstream interactions.|
|KYC-I-<br>04|MUST|KYC Orchestration MUST return structured error responses (HTTP status + error code + trace ID) to EIS/InvestiFi Gateway for all<br>failure scenarios.|
|KYC-I-<br>05|SHOULD|KYC Orchestration SHOULD support request idempotency via a caller-supplied requestId header to prevent duplicate processing<br>on retry.|



5.2 Integration with KYC - NCA






|ID|Priority|Requirement|
|---|---|---|
|KYC<br>-I-06|MUST|KYC Orchestration MUST invoke the KYC - NCA Data Ingestion API (KYC - NCA-02) to create a client record as part of the parallel<br>screening flow (Step 2a). All available client attributes MUST be included in the payload.|
|KYC<br>-I-07|MUST|KYC Orchestration MUST invoke the KYC - NCA Case API (KYC - NCA-03) to create an NCA case in New state when a Hit or High<br>risk is detected, passing initial screening and risk rating details keyed by UniqueClientID.|
|KYC<br>-I-08|MUST|KYC Orchestration MUST invoke the KYC - NCA Case API (KYC - NCA-04) to update the case status to Ready for Review after<br>receiving IDV confirmation from EIS/InvestiFi Gateway.|
|KYC<br>-I-09|MUST|KYC Orchestration MUST expose an API for KYC - NCA update case status by UniqueClientID.|
|KYC<br>-I-10|SHOULD|KYC Orchestration SHOULD handle KYC - NCA API errors gracefully, including retries with exponential back-off for transient<br>failures and dead-letter logging for persistent failures.|



5.3 Integration with NLS / NRTS






|ID|Priority|Requirement|
|---|---|---|
|KYC<br>-I-12|MUST|KYC Orchestration MUST invoke the NLS Real-Time Screening (NRTS) API using NLS Source ID OAP (2475), shared by both EIS<br>and PPR business lines.|
|KYC<br>-I-13|MUST|KYC Orchestration MUST handle NRTS responses in sub-second timeouts. If NRTS does not respond within the agreed SLA, KYC<br>Orchestration MUST return a timeout error to EIS/InvestiFi Gateway and log the event.|


5.4 Integration with CRRE






|ID|Priority|Requirement|
|---|---|---|
|KYC-<br>I-16|MUST|KYC Orchestration MUST invoke the CRRE API only after the NRTS screening result is received (sequential dependency:<br>screening before risk rating).|
|KYC-<br>I-17|MUST|The CRRE request MUST include both client data and the screening result (PEP,ADM).|
|KYC-<br>I-18|MUST|KYC Orchestration MUST handle the CRRE response in real time and include the risk rating in both the response to EIS/InvestFi<br>Gateway and the NCA case payload sent to KYC - NCA.|
|KYC-<br>I-19|SHOULD|KYC Orchestration SHOULD apply the same timeout and retry policy for CRRE as for NRTS, logging failures and returning a<br>structured error to EIS/InvestFi Gateway.|


## 6. Data Requirements

























|Data Element|Owner|Format|Used By KYC Orchestration For|
|---|---|---|---|
|UniqueClientID|EIS Gateway (source TBC – OI-<br>01)|String|Primary correlation key across all downstream calls and tracking records|
|Client Personal<br>Details|EIS Gateway|Structured object|Forwarded to KYC - NCA ingestion and CRRE|
|NLS Source ID|Shared: EIS & PPR|OAP (2475)|Included in every NRTS screening request|
|Screening Result|NLS / NRTS|Hit/No-Hit<br>+ context|Stored in tracking DB; passed to CRRE; loaded into KYC - NCA case; returned to EIS<br>/InvestFi|
|Risk Rating|CRRE|LOW/MEDIUM<br>/HIGH|Stored in tracking DB; loaded into KYC - NCA case; returned to EIS|
|Case ID|KYC - NCA|String|Stored in tracking DB to correlate KYC records with KYC - NCA cases|
|Case Status|KYC - NCA|Enum|Polled periodically to detect finalized cases for feedback delivery|
|Final Decision|Business Operations (via KYC -<br>NCA)|PROCEED/<br>REJECT|Included in KYC feedback sent to EIS/InvestiFi Gateway|
|IDV Reference|EIS/InvestiFi Gateway|String|Stored in tracking DB; passed to KYC - NCA on status update|


6.1 KYC Tracking Database


KYC Orchestration maintains its own internal tracking database. The minimum data model is:


|Field|Type|Description|
|---|---|---|
|uniqueClientId|String (PK)|Primary key; provided by EIS/InvestiFi Gateway|
|kycCaseId|String|NCA case ID assigned on case creation|
|businessLine|Enum|EIS or PPR|
|trackingStatus|Enum|PENDING  IN_PROGRESS  COMPLETED|
|screeningStatus|Enum|NO_HIT | HIT|
|riskRating|Enum|LOW | MEDIUM | HIGH | UNKNOWN|
|idvReference|String|IDV transaction reference from EIS/InvestiFi Gateway|
|idvConfirmedAt|DateTime|Timestamp of IDV confirmation|
|feedbackSentAt|DateTime|Timestamp when final feedback was dispatched to EIS/InvestiFi Gateway|
|createdAt|DateTime|Record creation timestamp|


|Col1|Col2|Col3|
|---|---|---|
|updatedAt|DateTime|Last update timestamp|

## 7. Non-Functional Requirements

7.1 Performance






|ID|Category|Requirement|
|---|---|---|
|KYC-<br>NF-01|Latency|The end-to-end response time for POST /screening/initiate (Steps 1–4) MUST be within a target of 3-5 seconds under normal<br>load, given that NRTS and CRRE both respond in sub-second time.|
|KYC-<br>NF-02|Latency|Individual downstream API calls (NRTS, CRRE, KYC - NCA ingestion) MUST each have a configurable timeout. Default: 30<br>seconds for NRTS and CRRE; 30 seconds for KYC - NCA.|
|KYC-<br>NF-03|Throughput|KYC Orchestration MUST support a minimum of 20 concurrent screening requests per second without degradation.|



7.2 Reliability & Resilience












|ID|Category|Requirement|
|---|---|---|
|KYC-<br>NF-<br>05|Availability|KYC Orchestration MUST achieve 99.5% uptime during business hours (07:00–22:00 CET).|
|KYC-<br>NF-<br>06|Retry|KYC Orchestration MUST implement retry with exponential back-off for all outbound API calls (KYC - NCA, NRTS, CRRE, EIS<br>feedback). Maximum retries: 3. Failures beyond retries MUST be dead-lettered and alerted.|
|KYC-<br>NF-<br>07|Partial<br>Failure|If the KYC - NCA ingestion call (Step 2a) fails but NRTS screening succeeds, KYC Orchestration MUST NOT return a success to<br>EIS/InvestFi Gateway. It MUST retry KYC - NCA ingestion and alert if all retries fail.|
|KYC-<br>NF-<br>08|Idempotency|All state-changing operations (case creation, status update, feedback delivery) MUST be idempotent. Reprocessing the same<br>event MUST NOT create duplicate records or send duplicate feedback.|



7.3 Security
















|ID|Category|Requirement|
|---|---|---|
|KYC-<br>NF-09|Authenticati<br>on|All inbound endpoints MUST be protected by OAuth 2.0 Client Credentials (Keycloak). Unauthenticated requests MUST be<br>rejected with HTTP 401.|
|KYC-<br>NF-10|Authorization|Scopes MUST be enforced per endpoint (e.g., kyc:screening:write for initiation; kyc:callbacks:write restricted to EIS/InvestiFi<br>Gateway and NLS integration service only).|
|KYC-<br>NF-11|Transport|All inter-service communication MUST use TLS 1.2 or higher. Certificates MUST be validated; self-signed certificates are NOT<br>permitted in production.|
|KYC-<br>NF-12|Data<br>Privacy|KYC Orchestration MUST NOT log full client PII in application logs. PII fields MUST be masked or tokenised in all log output.<br>Compliance with GDPR and BaFin data handling standards is  mandatory.|
|KYC-<br>NF-13|Audit Trail|All state transitions (case creation, status update, feedback delivery) MUST be written to an immutable audit log with timestamp,<br>actor, and previous/new state.|



7.4 Observability






|KYC-<br>NF-14|Tracing|KYC Orchestration MUST propagate a distributed trace ID (traceId) across all outbound calls to KYC - NCA, NRTS, and CRRE to<br>enable end-to-end request tracing.|
|---|---|---|
|KYC-<br>NF-15|Metrics|KYC Orchestration MUST expose metrics for: screening request rate, downstream call latency (per system), error rates per<br>endpoint, and pending case count in the tracking DB.|
|KYC-<br>NF-16|Alerting|Alerts MUST be configured for: downstream system unavailability, retry exhaustion, polling job failures, and SLA breach risk on<br>pending cases.|


## 8. Constraints














|ID|Type|Constraint|
|---|---|---|
|KYC-<br>C-01|Techni<br>cal|KYC Orchestration MUST be implemented as a Spring Boot microservice aligned with the existing DWS microservices platform stack.|
|KYC-<br>C-02|Integra<br>tion|NLS Source ID OAP (2475) MUST be used for all NRTS screening calls for both EIS and PPR business lines. This is a fixed<br>configuration value.|
|KYC-<br>C-03|Data|KYC Orchestration MUST NOT store unmasked PII beyond what is operationally necessary. PII retention periods MUST comply with<br>GDPR Article 5(1)(e).|
|KYC-<br>C-04|Process|KYC Orchestration MUST NOT close or finalize NCA cases itself. Case finalization is exclusively the responsibility of Business<br>Operations teams acting through KYC - NCA.|
|KYC-<br>C-05|Process|KYC Orchestration MUST NOT bypass the KYC - NCA Case API for case management. Direct database access to KYC - NCA is<br>prohibited.|
|KYC-<br>C-06|Depen<br>dency|The parallel invocation of KYC - NCA ingestion (Step 2a) and NRTS screening (Step 2b) MUST be genuinely concurrent. Sequential<br>execution is not acceptable due to sub-second latency requirements.|


## 9. Assumptions






|ID|Assumption|
|---|---|
|KYC<br>-A-<br>01|EIS/InvestFi Gateway is operational and able to provide a valid UniqueClientID in every screening request. If UniqueClientID generation is KYC<br>Orchestration's responsibility, requirements KYC-F-01 and KYC-I-03 will need revision — see<br>.<br>OI-01|
|KYC<br>-A-<br>02|NRTS is available and consistently meets sub-second response SLA under expected load.|
|KYC<br>-A-<br>03|CRRE accepts combined client data and screening result payloads in a single request.|
|KYC<br>-A-<br>04|KYC - NCA will be enhanced with the required API changes (KYC - NCA-01 through KYC - NCA-05) as a parallel workstream. KYC<br>Orchestration development assumes these APIs will be ready for integration testing.|
|KYC<br>-A-<br>05|Both EIS and PPR business lines use identical screening and risk-rating workflows. Any business-line-specific divergence is out of scope for v1.|
|KYC<br>-A-<br>06|The NLS final feedback file is delivered as a file push to KYC - NCA.|
|KYC<br>-A-<br>07|dbDocs integration is out of scope for KYC Orchestration. Document access by KYC Analysts is handled directly via dbDocs UI using<br>UniqueClientID.|


## 10. Open Items & Decisions Required












|ID|Question|Impact on KYC Orchestration|Owner|Priority|
|---|---|---|---|---|
|OI-<br>01|Who generates the UniqueClientID — EIS/InvestiFi<br>Gateway or KYC Orchestration?|Determines whether uniqueClientId is an input to or output of the screening<br>initiation endpoint. Affects KYC-F-01, KYC-I-03, and the KYC Tracking DB<br>schema.|EIS / KYC<br>Team / AFC|MUST|
|OI-<br>02|What is the agreed SLA for periodic case polling in<br>Step 11?|Drives the polling interval configuration (KYC-F-22) and the alerting thresholds<br>(KYC-NF-16).|Business /<br>EIS|MUST|
|OI-<br>04|Does EIS Gateway require a push (webhook) for<br>KYC feedback, or will it poll for results?|If push is required, KYC Orchestration must implement an outbound webhook<br>call instead of (or in addition to) the feedback endpoint (KYC-F-23).|EIS<br>Gateway<br>Team|SHOULD|


## 11. Requirements Traceability Matrix

Maps each functional requirement to its NCA process step, the downstream system it touches, and the API operation it maps to.

|Req ID|NCA Step|Downstream System|API Operation|
|---|---|---|---|
|KYC-F-01|Step 1|EIS Gateway (inbound) / InvestFi Gateway|POST /screening/initiate|
|KYC-F-02|Step 2|KYC - NCA + NLS/NRTS (parallel)|KYC - NCA ingestion API + NRTS API|
|KYC-F-03|Step 2a|KYC - NCA|KYC - NCA Data Ingestion API (KYC - NCA-02)|
|KYC-F-04–05|Step 2b|NLS/NRTS|NRTS Screening API|
|KYC-F-06–07|Step 3|CRRE|CRRE Risk Rating API|
|KYC-F-08|Step 4|EIS/InvestiFi Gateway (outbound)|Response to POST /screening/initiate|
|KYC-F-10–14|Step 5|KYC - NCA + KYC Tracking DB|KYC - NCA Case API (KYC - NCA-03); internal DB write|
|KYC-F-15–17|Steps 6–7|NLS (inbound) + KYC - NCA|POST /callbacks/nls-feedback KYC - NCA Case API|
|KYC-F-18|Step 8|EIS/InvestiFi Gateway (inbound)|POST /callbacks/idv-confirmation|
|KYC-F-19–20|Step 9|KYC - NCA|PATCH /cases/{id}/ready-for-review KYC - NCA Case API (KYC - NCA-04)|
|KYC-F-21–26|Step 11|KYC - NCA + EIS Gateway|GET /cases + POST /feedback/{id} KYC - NCA Case API (KYC - NCA-05)|


## 12. Revision History


|Date|Author|Description|Ver.|
|---|---|---|---|
|3/23/2026|DWS – KYC Platform Team|Initial elicitation from NCA Requirements Document v1.0|1|


