# JIRA Import Guide — KYC Orchestration Stories

## Files

| File | Contents |
|---|---|
| `JIRA_STORIES.csv` | 9 Epics + 40 Stories ready for JIRA bulk import |

---

## Story Summary

| Epic | Stories | Points | Priority |
|---|---|---|---|
| KYC Screening Orchestration & Real-Time Flow | 7 | 32 | High |
| KYC On-Hold Detection & NCA Case Creation | 4 | 13 | High |
| KYC NLS Final Feedback Processing | 3 | 13 | High |
| KYC IDV Confirmation & Case Status Transition | 3 | 10 | High |
| KYC Case Finalization & Feedback Delivery | 5 | 20 | High / Medium / Low |
| KYC API Integration & Authentication | 5 | 17 | High / Medium |
| KYC Security & Data Privacy Compliance | 4 | 13 | High |
| KYC Performance, Reliability & Resilience | 5 | 21 | High |
| KYC Observability & Alerting | 3 | 9 | High / Medium |
| **TOTAL** | **40** | **148** | |

---

## How to Import into JIRA Cloud

### Step 1 — Prepare your project
1. Open your JIRA project and confirm it has the **Epic**, **Story** issue types enabled.
2. Note your project key (e.g. `KYCO`).

### Step 2 — Import the CSV
1. Go to **Project Settings → Import Issues** (JIRA Cloud) or  
   **Issues → Import Issues from CSV** (JIRA Software navigation bar).
2. Upload `JIRA_STORIES.csv`.
3. On the **Map Fields** screen, map the CSV columns as follows:

| CSV Column | JIRA Field |
|---|---|
| `Summary` | Summary |
| `Issue Type` | Issue Type |
| `Priority` | Priority |
| `Description` | Description |
| `Acceptance Criteria` | Acceptance Criteria *(custom field — see note below)* |
| `Labels` | Labels |
| `Epic Name` | Epic Name *(Epic issues only)* |
| `Epic Link` | Epic Link |
| `Story Points` | Story Points |
| `Component/s` | Component/s |

> **Acceptance Criteria field:** Many JIRA projects use a custom field for this. If your project does not have it, map it to **Description** instead (the import script will append it) — or add the field via **Project Settings → Fields** before importing.

### Step 3 — Import order matters
JIRA must create **Epics before Stories** so Epic Links resolve correctly.  
The CSV is already ordered: all 9 Epic rows appear first, followed by 40 Story rows.

### Step 4 — Verify after import
After import, verify:
- All 9 Epics are created and visible on the Roadmap.
- All 40 Stories are linked to their parent Epic.
- Labels (e.g. `KYC-F-01`, `MUST`) are applied for traceability back to `kycOrc.md`.

---

## How to Import into JIRA Data Center / Server

1. Go to **Issues → Import Issues from CSV**.
2. Follow the same column mapping as above.
3. If **Epic Link** is not available as a mapped field, create the Epics first, note their generated issue keys (e.g. `KYCO-1`), and manually update the `Epic Link` column with the issue keys before re-importing the Story rows.

---

## Label → Requirement Traceability

Each story carries one or more labels that map directly to requirement IDs in `kycOrc.md`:

| Label prefix | Source |
|---|---|
| `KYC-F-XX` | Functional requirement |
| `KYC-I-XX` | Integration requirement |
| `KYC-NF-XX` | Non-functional requirement |
| `KYC-C-XX` | Constraint |
| `MUST` / `SHOULD` / `MAY` | MoSCoW priority from requirements doc |

---

## Priority Mapping

| kycOrc.md Priority | JIRA Priority |
|---|---|
| MUST | High |
| SHOULD | Medium |
| MAY | Low |

---

## Suggested Sprint Plan

| Sprint | Epics | Focus |
|---|---|---|
| Sprint 1 | Screening Orchestration, On-Hold Detection, API Integration & Auth | Core happy-path and on-hold flow end-to-end |
| Sprint 2 | IDV Confirmation, Security & Privacy, Performance & Reliability | Callbacks, security hardening, NFR baseline |
| Sprint 3 | NLS Feedback, Case Finalization, Observability | Async feedback flows, metrics, alerting |

---

## Open Items to Resolve Before Sprint 1

These open items from `kycOrc.md` affect story scope and must be resolved before refinement:

| ID | Question | Affected Stories |
|---|---|---|
| OI-01 | Who generates UniqueClientID — EIS/InvestiFi or KYC Orchestration? | S01 (Expose initiation API), S03 (Forward client data), S10 (Tracking record) |
| OI-02 | What is the agreed SLA for periodic case polling (Step 11)? | S19 (Poll KYC-NCA), S40 (Alerting thresholds) |
| OI-04 | Does EIS Gateway require webhook push or will it poll for KYC feedback? | S20 (Deliver feedback), S22 (At-least-once delivery) |
