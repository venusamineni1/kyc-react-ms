# KYC Orchestration Service Scaffolding

This plan outlines the scaffolding for the new `kyc-orchestration` service. The service acts as a central coordinator between EIS/InvestiFi Gateways and downstream KYC systems, exposing a REST-compliant endpoint for KYC prechecks. It orchestrates concurrent calls to the `viewer` (user creation) and `screening-service`, subsequently passing results sequentially to the `risk-service`. 

It ensures secure, auditable, and resilient execution with full exception handling, comprehensive logging, encrypted database persistence, and a highly latency-optimized execution topology.

## Proposed Changes

### Root Configuration
To integrate the new microservice into the existing project structure, the following changes will be made:

#### [MODIFY] settings.gradle
```gradle
include 'kyc-orchestration'
```

#### [MODIFY] pom.xml
```xml
<modules>
    <!-- ... existing modules ... -->
    <module>kyc-orchestration</module>
</modules>
```

### kyc-orchestration Module Configuration

#### [NEW] kyc-orchestration/build.gradle
Gradle configuration incorporating standard web dependencies, Eureka, **Data JPA**, and an in-memory db driver driver for scaffolding:
- `spring-boot-starter-web`
- `spring-cloud-starter-netflix-eureka-client`
- `spring-boot-starter-data-jpa`
- `h2` (or your preferred relational database driver)
- `lombok`

#### [NEW] src/main/resources/application.yml
- Application port: `8086`.
- Application name: `kyc-orchestration`.
- Eureka client URL configuration.
- **Datasource Configuration**: SQL settings and Spring JPA Hibernate dialet.

### Application Logic

#### [NEW] src/main/java.../config/RestTemplateConfig.java
Expose an `@LoadBalanced` `RestTemplate` bean to perform inter-service HTTP requests across registered Eureka hosts.

#### [NEW] DTOs
- `KycPrecheckRequest`: `uniqueClientID`, `firstName`, `middleName`, `lastName`, `title`, `dob`.
- `KycPrecheckResponse`: 
  - `status`, `screeningResult`, `hitContext`, `riskRating`
  - **Traceable IDs**: `userId`, `screeningRequestId`, `riskRequestId`
  - **Timestamps**: `screeningStartAt`, `screeningEndAt`, `riskStartAt`, `riskEndAt`
- **`ErrorResponse`**: Standardized DTO containing timestamp, status code, error, and descriptive message for gracious failures.

#### [NEW] API Clients
- `ViewerClient`: `http://VIEWER/api/prospects/onboard`
- `ScreeningClient`: `http://SCREENING-SERVICE/api/internal/screening/initiate`
- `RiskClient`: `http://RISK-SERVICE/api/internal/risk/calculate`

#### [NEW] Logging & Exception Handling (`src/main/java.../exception/`)
- **`GlobalExceptionHandler`**: A `@ControllerAdvice` component that globally hooks into downstream REST errors. It ensures external clients receive clean JSON failure states rather than raw internal stack traces.

### Database, Auditability & Encryption

#### [NEW] src/main/java.../crypto/AttributeEncryptor.java
An implementation of `AttributeConverter<String, String>` using AES/GCM encryption. This seamlessly intercepts JPA property transitions to encrypt sensitive user data before writing to the database and decrypt upon reading.

#### [NEW] src/main/java.../domain/KycTransactionAudit.java
A `@Entity` capturing the transaction timeline.
- Tracked fields: `id`, `transactionId`, `uniqueClientID`, `orchestrationStatus`, `createdAt`.
- **Trace Tracking**: `viewerUserId`, `screeningRequestId`, `riskRequestId`.
- **Latency Tracking**: `screeningStartAt`, `screeningEndAt`, `riskStartAt`, `riskEndAt`.
- **Encrypted Fields**: Client details (`firstName`, `lastName`, `dob`) marked with `@Convert(converter = AttributeEncryptor.class)` ensuring zero clear-text PII touches disk. 

#### [NEW] src/main/java.../repository/KycTransactionAuditRepository.java
A standard `JpaRepository` interface for persisting `KycTransactionAudit` objects.

#### [NEW] src/main/java.../controller/KycOrchestrationController.java
Expose `POST /api/v1/screening/initiate`. Defers to the service layer. Annotated with `@Slf4j` for lifecycle logging.

#### [NEW] src/main/java.../service/KycOrchestrationService.java
Orchestrates the sequence using highly latency-optimized non-blocking workflows:
1. **Initial Audit Setup (Async)**: Offload the initial "PENDING" audit record insert to a background `CompletableFuture`. Main thread continues instantly.
2. **Parallel Trigger**:
   - `ViewerClient` executes. Uses `.exceptionally(...)` to safely swallow errors (meaning user creation acts as a non-critical side effect without crashing the endpoint).
   - `ScreeningClient` executes. 
3. **Optimized Synchronization**: Await *only* the `ScreeningClient` completion to unlock the risk stage.
4. **Sequential Trigger**: Hand off the screening hit payload immediately to the `RiskClient`.
5. **Viewer Catch-up**: After `RiskClient` returns, retrieve the Viewer `userId`. Since it ran completely in parallel alongside Screening + Risk, its footprint adds zero latency unless it is the single slowest execution block.
6. **Final Database Audit (Async)**: Pipe the final transaction details into `.thenAcceptAsync()` on the initial Audit future. This saves the execution times and tracker IDs sequentially into the DB on a background fork-join pool, allowing the API endpoint to instantly `return 200 OK`.
