# IssueFlow Retry and Durable Outbox Requirements

## 1. Purpose

Extend IssueFlow with a small, production-inspired retry workflow for outbound service calls.

The goal is to demonstrate safe handling of transient downstream failures without turning IssueFlow into a large distributed system.

This change should add:

- Request validation before outbound work is created.
- A durable, database-backed outbound job queue.
- Retry classification based on failure type.
- Exponential backoff with bounded retry attempts.
- Idempotency protection for each logical outbound operation.
- A transactional outbox style workflow so business state changes and outbound work intent are persisted together.
- Issue history entries that make retry behavior visible and explainable.
- Automated tests for normal, retryable, non-retryable, duplicate, restart, and max-attempt scenarios.

The implementation must remain easy to explain in a technical interview.

---

## 2. Existing Application Constraints

IssueFlow already uses:

### Frontend

- React 19
- TypeScript 6
- Vite 8
- Yarn
- Vitest

### Backend

- Java 17
- Spring Boot 3.4
- Maven
- Spring Data JPA / Hibernate
- SQLite
- JUnit 5
- OpenAPI / Swagger

### Architectural Rules

- Frontend and backend remain separate processes.
- Frontend communicates with backend only through REST/JSON.
- Frontend must not access the database directly.
- Backend must not render frontend HTML.
- Backend entities must not be exposed directly through REST APIs.
- Existing layering should remain clear: Controller -> Service -> Repository.
- Business logic belongs in backend services, not controllers or frontend components.

---

## 3. Engineering Workflow Requirement

Cursor must not immediately implement this change.

Cursor must first:

1. Review the current repository structure and existing `REQUIREMENTS.md`.
2. Identify all files and components that are likely to be affected.
3. Produce a written implementation plan.
4. Explain any architectural tradeoffs.
5. Identify assumptions or conflicts with the current codebase.
6. Identify database migration or schema implications.
7. Identify required unit, integration, and frontend tests.
8. Wait for approval before implementation if operating interactively.

The implementation plan should favor the smallest coherent design that satisfies these requirements.

Do not add infrastructure merely because it is common in larger systems.

---

## 4. Use Case

Add an outbound operation representing an external escalation notification.

Example logical event:

> An issue is escalated or reaches a qualifying state and IssueFlow must notify an external service.

The external service is intentionally simulated for this demo.

The implementation must support configurable simulated responses so retry behavior can be demonstrated and tested without depending on an actual third-party service.

The simulated external service should be able to produce:

- Success
- Timeout
- HTTP 408
- HTTP 429
- HTTP 400
- HTTP 401
- HTTP 403
- HTTP 404
- HTTP 409
- HTTP 422
- HTTP 500
- HTTP 502
- HTTP 503
- HTTP 504

Do not build a real payment integration.

The credit-card example is the motivating failure scenario, not the required domain implementation.

---

## 5. Core Design Requirement

The outbound workflow must use a durable database-backed job record.

Do not introduce:

- Kafka
- RabbitMQ
- SQS
- Redis
- Kubernetes
- A separate microservice
- External workflow engines

SQLite and the existing Spring Boot process are sufficient for this demo.

A Spring scheduled worker or equivalent simple backend worker should process pending jobs.

---

## 6. Transactional Outbox Behavior

When a business operation requires an outbound notification, the system must persist both:

1. The relevant IssueFlow business state change.
2. The outbound job representing the required external call.

These writes must occur in the same database transaction whenever they are logically part of the same operation.

Example:

```text
BEGIN TRANSACTION
    update issue
    insert outbound job
COMMIT
```

The external service call must not occur inside that same database transaction.

A separate worker processes the persisted outbound job after commit.

This is intended to prevent the failure case where IssueFlow successfully commits a business state change but crashes before recording that an external notification is required.

---

## 7. Outbound Job Model

Create a durable outbound job model.

The exact class and table names may follow existing project naming conventions.

The record should contain at minimum:

```text
id
operationType
issueId
idempotencyKey
status
attemptCount
nextAttemptAt
lastAttemptAt
lastHttpStatus
lastError
createdAt
updatedAt
completedAt
```

Optional fields may be added if clearly justified.

### Required Status Values

Use a clear enum or equivalent typed representation.

Suggested values:

```text
PENDING
PROCESSING
RETRY_SCHEDULED
SUCCEEDED
FAILED
```

A separate `DEAD` status may be used instead of `FAILED` for exhausted retries, but only if the distinction improves clarity.

Avoid unnecessary status complexity.

---

## 8. Idempotency Requirements

Each logical outbound operation must have a stable idempotency key.

The idempotency key must:

- Represent the logical operation, not an individual retry attempt.
- Remain unchanged across retries.
- Prevent duplicate outbound jobs for the same logical operation.
- Be enforced by backend logic.
- Preferably also be protected by a database uniqueness constraint.

If the same logical operation is requested twice, IssueFlow must not create or execute a second independent outbound operation.

The implementation should be able to explain how this protects against duplicate effects when a caller does not know whether the previous attempt succeeded.

Do not treat retry attempt number as part of the idempotency key.

---

## 9. Retry Classification

The worker must classify failures as retryable or non-retryable.

### Retryable

Retry at minimum:

- Network exception
- Connection timeout
- HTTP 408 Request Timeout
- HTTP 429 Too Many Requests
- HTTP 500 Internal Server Error
- HTTP 502 Bad Gateway
- HTTP 503 Service Unavailable
- HTTP 504 Gateway Timeout

### Non-Retryable

Do not automatically retry most other 4xx responses.

At minimum, the following should fail without automatic retry:

- HTTP 400 Bad Request
- HTTP 401 Unauthorized
- HTTP 403 Forbidden
- HTTP 404 Not Found
- HTTP 422 Unprocessable Entity

HTTP 409 Conflict may be handled according to the simulated operation semantics, but the implementation plan must explain the chosen behavior.

Retry classification must exist in backend service logic and must be testable independently.

Do not scatter response-code decisions across controllers, repositories, and UI code.

---

## 10. Retry Policy

Retry attempts must be bounded.

Default maximum:

```text
5 total attempts
```

Use exponential or progressively increasing backoff.

A reasonable demo sequence is:

```text
Attempt 1: immediately
Attempt 2: +5 seconds
Attempt 3: +15 seconds
Attempt 4: +45 seconds
Attempt 5: +120 seconds
```

These values should be configurable in backend application configuration rather than hard-coded throughout the code.

For automated tests, timing must be injectable, configurable, or otherwise testable without requiring tests to sleep for real retry intervals.

Do not write tests that wait minutes for retry delays.

---

## 11. HTTP 429 Handling

If a simulated HTTP 429 response includes a `Retry-After` value, IssueFlow should honor it when reasonable.

If no `Retry-After` value is present, use the normal retry policy.

The implementation must still enforce the maximum-attempt limit.

---

## 12. Ambiguous Outcome Handling

The design should acknowledge the case where an external request may have succeeded even though IssueFlow did not receive the response.

Examples:

- Connection drops after request transmission.
- Timeout occurs while waiting for the response.
- Process interruption occurs after the remote service accepts the request.

For this demo:

- The stable idempotency key is the primary duplicate-protection mechanism.
- The simulated external service must recognize repeated calls with the same idempotency key as the same logical operation.
- Repeated attempts must not create multiple simulated side effects.

If a status lookup or reconciliation endpoint is added, it must remain small and clearly justified.

A full reconciliation subsystem is not required.

---

## 13. Worker Behavior

Create a backend worker that periodically looks for eligible outbound jobs.

The worker must:

1. Find jobs that are pending or retry-scheduled.
2. Only select jobs whose `nextAttemptAt` is due.
3. Mark the job as processing before making the outbound call.
4. Execute the simulated external call.
5. Record success or failure.
6. Increment attempt count.
7. Schedule retry if the error is retryable and attempts remain.
8. Mark the job failed if the error is non-retryable.
9. Mark the job failed when retry attempts are exhausted.
10. Record relevant details in IssueFlow history.

The design should reduce the chance that multiple worker executions process the same job concurrently.

Because IssueFlow is a single-process SQLite demo, the solution may be modest.

Do not over-engineer distributed locking.

Cursor should still explain the concurrency limitation and how the design would need to change in a horizontally scaled production system.

---

## 14. Restart Durability

Retry state must survive application restart.

If the Spring Boot process stops after a retry is scheduled, the job must still exist in SQLite and become eligible when the application starts again.

Do not store retry state only in memory.

---

## 15. Validation

Existing frontend and backend validation must remain intact.

Any new API that creates or triggers an outbound operation must:

- Validate required input on the frontend where appropriate.
- Validate again on the backend.
- Reject invalid requests before outbound work is created.
- Return structured API errors consistent with current IssueFlow conventions.

Frontend validation is for user experience.

Backend validation is authoritative.

---

## 16. API Requirements

Expose only the endpoints necessary to demonstrate and inspect the workflow.

Suggested endpoints:

```text
POST /api/issues/{issueId}/escalation-notification
GET  /api/issues/{issueId}/outbound-jobs
GET  /api/outbound-jobs/{jobId}
```

The exact endpoint naming may be adjusted to fit the current codebase.

The trigger endpoint should be idempotent for the same logical escalation event.

Response DTOs should expose useful demo information without exposing JPA entities.

Suggested response information:

```text
jobId
operationType
status
attemptCount
nextAttemptAt
lastHttpStatus
lastError
createdAt
updatedAt
completedAt
```

---

## 17. Simulated External Service

Create a small simulated outbound client/service.

It must not require internet access.

It should support deterministic demo/test modes such as:

```text
ALWAYS_SUCCEED
FAIL_ONCE_THEN_SUCCEED
FAIL_TWICE_THEN_SUCCEED
ALWAYS_503
ALWAYS_400
ALWAYS_TIMEOUT
RATE_LIMIT_THEN_SUCCEED
```

The exact mechanism may be configuration, request parameter in a demo-only endpoint, test fixture, or internal simulator.

Production-style domain services should not depend directly on test classes.

Keep simulation concerns isolated.

---

## 18. Issue History

Retry activity should be visible in issue history.

Add history entries for meaningful events such as:

```text
Escalation notification queued
Escalation notification attempt 1 failed: HTTP 503
Escalation notification retry scheduled
Escalation notification succeeded on attempt 2
Escalation notification permanently failed after 5 attempts
```

Do not expose stack traces or sensitive internal exception details to the UI.

History text should be concise and useful for debugging and demonstration.

---

## 19. Frontend Requirements

Add a small UI section to the Issue Detail page for outbound notification status.

It should display:

- Current outbound job status
- Attempt count
- Last response status if available
- Last safe error message if available
- Next retry time if scheduled
- Completion time if successful
- A concise history of attempts

Allow the user to trigger the demo escalation notification if appropriate.

Do not move retry logic into the frontend.

The frontend may refresh status by:

- Manual refresh
- Existing page refresh behavior
- Lightweight polling if already compatible with the application

Do not add WebSockets or SSE solely for this feature.

---

## 20. Configuration

Add sanitized example configuration for all new environment variables or application properties.

Examples may include:

```text
OUTBOUND_RETRY_MAX_ATTEMPTS=5
OUTBOUND_RETRY_INITIAL_DELAY_SECONDS=5
OUTBOUND_WORKER_INTERVAL_SECONDS=2
OUTBOUND_SIMULATION_MODE=FAIL_ONCE_THEN_SUCCEED
```

Use configuration names consistent with the current project.

If `.env` or application configuration changes, update the existing sanitized example configuration file.

Do not commit secrets.

---

## 21. Logging and Observability

Backend logs should make the workflow understandable without being noisy.

Log at minimum:

- Job created
- Attempt started
- Retryable failure
- Non-retryable failure
- Retry scheduled
- Job succeeded
- Retry attempts exhausted

Include identifiers such as:

```text
jobId
issueId
idempotencyKey
attemptCount
```

Do not log sensitive payloads.

---

## 22. Testing Requirements

Maintain or improve the existing project test quality.

### Backend Unit Tests

Test at minimum:

- Retry classifier
- Retry delay calculation
- Maximum attempt behavior
- Idempotency key behavior
- Duplicate job prevention
- Non-retryable 4xx behavior
- Retryable timeout behavior
- Retryable 408 behavior
- Retryable 429 behavior
- Retryable 5xx behavior
- Retry-After handling
- Success behavior
- Exhausted retry behavior

### Backend Integration Tests

Test at minimum:

- Business state change and outbound job persist atomically.
- No outbound job is left behind if the transaction rolls back.
- Retry state persists in SQLite.
- Worker processes due jobs.
- Worker ignores jobs whose next retry time has not arrived.
- Duplicate trigger request does not create duplicate logical work.
- Simulated repeated delivery with same idempotency key does not create duplicate side effects.
- History records are created correctly.

### Frontend Tests

Test at minimum:

- Outbound job status renders correctly.
- Retry information renders correctly.
- Failure state renders correctly.
- Success state renders correctly.
- Trigger action handles loading and error states.
- UI does not duplicate retry logic.

### Test Design

Do not use real-time sleeps for exponential backoff tests.

Abstract or inject clock/timing behavior where necessary.

Tests must remain deterministic and fast.

---

## 23. Error Handling

Use the project's centralized backend error handling.

Do not create one-off controller try/catch logic unless required for a well-defined boundary.

Errors returned to the frontend should remain structured and safe.

The application should distinguish:

```text
validation failure
non-retryable outbound failure
retryable outbound failure
retry exhausted
internal application failure
```

Do not expose raw exception stack traces through APIs.

---

## 24. Code Quality Requirements

- Java must remain Java 17.
- Backend package management must remain Maven.
- Frontend package management must remain Yarn.
- Do not introduce npm commands or npm lock files.
- Keep constants centralized according to the project's existing constants structure.
- Do not hard-code retry status codes, retry counts, or delay values in multiple places.
- Prefer enums and typed DTOs over stringly typed logic.
- Keep services focused and testable.
- Do not bypass existing architecture for convenience.
- Fix underlying design problems rather than adding one-off hacks.
- Avoid unnecessary abstractions.
- Avoid unnecessary dependencies.
- Do not add emojis to code, comments, logs, documentation, or generated UI text.
- Do not use em dashes in code, comments, logs, or documentation.

---

## 25. Security and Safety

Although the external service is simulated, design it as if duplicate side effects could matter.

The implementation must:

- Validate input.
- Protect idempotency.
- Avoid unbounded retries.
- Avoid logging sensitive payload data.
- Avoid exposing internal exception details.
- Prevent duplicate logical jobs where practical.
- Fail safely when retry limits are exhausted.

Do not add real payment credentials, secrets, or third-party API keys.

---

## 26. Scope Limits

This feature is intended to demonstrate failure handling and retry design.

Do not add the following unless separately approved:

- Kafka
- RabbitMQ
- AWS SQS
- Redis
- Kubernetes
- Docker solely for this change
- New cloud infrastructure
- New microservices
- Distributed tracing platform
- External payment processor
- Full reconciliation engine
- Complex workflow orchestration framework

The final implementation should still be understandable in a short technical interview demo.

---

## 27. Interview Demonstration Goal

After implementation, the project should support a short demonstration like this:

1. Trigger an escalation notification.
2. Simulate a transient 503 failure.
3. Show the failed attempt recorded.
4. Show the job scheduled for retry.
5. Show a later attempt succeed.
6. Show the stable idempotency key across attempts.
7. Show the issue history containing the retry lifecycle.
8. Optionally restart the backend before the retry and show that the pending job survives.

The explanation should be simple:

> The application validates the request, commits the business state and outbound intent together, then a separate worker performs the external call. Retryable failures use bounded backoff, permanent failures stop immediately, and the same idempotency key is reused across retries so repeating the request cannot create a duplicate logical side effect.

---

## 28. Acceptance Criteria

The feature is complete when all of the following are true:

- [ ] Outbound work is stored durably in SQLite.
- [ ] Business state and outbound intent can be persisted atomically.
- [ ] A worker processes due outbound jobs.
- [ ] Retryable and non-retryable failures are classified correctly.
- [ ] Retry attempts are bounded.
- [ ] Backoff is configurable.
- [ ] HTTP 429 can honor Retry-After.
- [ ] Idempotency key remains stable across retries.
- [ ] Duplicate logical triggers do not create duplicate logical work.
- [ ] Retry state survives backend restart.
- [ ] Exhausted retries transition to a permanent failure state.
- [ ] Issue history shows important retry lifecycle events.
- [ ] UI can show job state and retry information.
- [ ] Backend tests cover retry classification and lifecycle behavior.
- [ ] Integration tests cover transactionality, persistence, and idempotency.
- [ ] Frontend tests cover status rendering and trigger behavior.
- [ ] No real external payment provider is used.
- [ ] No message broker or unnecessary infrastructure is added.
- [ ] Existing IssueFlow behavior remains functional.
- [ ] Existing API and architectural conventions remain intact.
- [ ] New configuration is reflected in sanitized example configuration.
- [ ] Cursor provides and reviews an implementation plan before coding.

---

## 29. Cursor Planning Questions

Before implementation, Cursor should explicitly answer:

1. Where in the current IssueFlow architecture should outbound job creation live?
2. Which existing business action should create the outbound job?
3. How will the business update and outbound job insert share one transaction?
4. What JPA entities, DTOs, repositories, services, and controllers need to be added or changed?
5. How will idempotency uniqueness be enforced?
6. How will the worker avoid processing the same job twice in the current single-instance design?
7. How will time be abstracted so retry tests do not sleep?
8. How will retry policy configuration be represented?
9. How will simulated external responses be isolated from production-style domain logic?
10. How will retry events be represented in issue history?
11. What frontend components and API client changes are required?
12. What database migration or seed changes are required?
13. What existing tests might need updates?
14. What new tests are required?
15. What are the known limitations of this SQLite single-process implementation?
16. How would the design differ in a horizontally scaled production environment?
17. Can any proposed dependency or abstraction be removed while still meeting the requirements?

---

## 30. Final Implementation Review

After implementation, Cursor must perform a final review against this requirements document.

The final review should identify:

- Any requirement not implemented.
- Any requirement implemented differently from the plan.
- Any unnecessary complexity introduced.
- Any duplicated retry logic.
- Any hard-coded retry behavior that should be configuration.
- Any concurrency or transaction concerns.
- Any untested failure path.
- Any frontend logic that should belong in the backend.
- Any idempotency weakness.
- Any architectural shortcut that should be corrected before the feature is considered complete.

The feature should not be marked complete solely because tests pass.

The final implementation must also be reviewed against the intended behavior and failure scenarios in this document.

---

## 31. Main README Documentation Requirement

The feature must be fully documented in the repository's main root `README.md`.

Cursor must update the existing main README rather than creating a separate feature README unless a separate document is clearly justified in addition to the main README.

The main README must remain concise enough to serve as the primary project introduction, but it must document this feature thoroughly enough that a hiring manager, senior engineer, or interviewer can understand:

- Why the retry/outbox feature exists.
- The failure scenario it is designed to address.
- The difference between validation failures, retryable failures, and permanent failures.
- Why blindly retrying a financial-style operation can create duplicate side effects.
- How stable idempotency keys prevent duplicate logical operations.
- How the durable SQLite-backed outbound job queue works.
- How the transactional outbox-style flow keeps the business state change and outbound intent consistent.
- How the scheduled worker processes pending jobs.
- Which HTTP responses and failure types are retried.
- Which failures are not retried.
- How bounded retry attempts and backoff work.
- How HTTP 429 / `Retry-After` is handled.
- How retry state survives backend restarts.
- How retry activity is exposed in issue history and the UI.
- The limitations of the current single-process SQLite implementation.
- How a larger horizontally scaled production system could evolve the design.

### Required README Architecture/Flow Documentation

Add or update a Mermaid diagram showing the outbound workflow.

The diagram should communicate approximately:

```text
User / Issue Action
        |
        v
Spring Service Transaction
   |               |
   v               v
Update Issue   Create Outbound Job
        \         /
          COMMIT
             |
             v
       Scheduled Worker
             |
             v
    Simulated External Call
       /             \
   Success           Failure
      |                 |
      v                 v
  SUCCEEDED       Retryable?
                    /      \
                  Yes       No
                   |         |
                   v         v
            Schedule Retry  FAILED
```

The exact Mermaid syntax may be adjusted for readability.

### Required README Retry Policy Table

Include a concise table documenting retry behavior.

| Failure | Retry? | Behavior |
|---|---|---|
| Network timeout | Yes | Retry with bounded backoff |
| HTTP 408 | Yes | Retry with bounded backoff |
| HTTP 429 | Yes | Honor `Retry-After` when provided |
| HTTP 500/502/503/504 | Yes | Retry with bounded backoff |
| HTTP 400 | No | Fail immediately |
| HTTP 401/403 | No | Fail immediately |
| HTTP 404 | No | Fail immediately |
| HTTP 422 | No | Fail immediately |
| Retry limit exhausted | No | Mark permanently failed |

Adjust the table if implementation decisions differ, but document the final behavior accurately.

### Required README Idempotency Explanation

Include a short explanation of the ambiguous-outcome case:

```text
The external operation may succeed even if IssueFlow does not receive the response.
Retry attempts therefore reuse the same idempotency key so the same logical
operation cannot create duplicate simulated side effects.
```

Do not claim this makes the system exactly-once.

Use accurate terminology such as:

- idempotent operation
- duplicate protection
- at-least-once retry attempts with idempotent handling

Avoid claiming exactly-once delivery unless the implementation truly provides and proves it.

### Required README Transactional Outbox Explanation

Explain that IssueFlow does not call the external service while the primary business transaction is open.

Instead:

```text
1. Persist the business state change.
2. Persist the outbound intent in the same database transaction.
3. Commit.
4. Let a separate worker perform the external call.
```

Explain why this is safer than:

```text
update database
call external API
hope neither side fails in between
```

### Required README Demo Instructions

Add a short demo walkthrough that lets an interviewer reproduce the feature.

At minimum document how to demonstrate:

1. A successful outbound notification.
2. A transient failure followed by a successful retry.
3. A non-retryable failure.
4. Retry exhaustion.
5. Stable idempotency across retries.
6. Retry/history visibility in the UI.
7. Restart durability if practical to demonstrate.

Document any configuration or simulation mode required for these scenarios.

### Required README Configuration Documentation

Document all new configuration properties/environment variables in the main README.

For each configuration item, include:

- Name
- Purpose
- Default value if applicable
- Example safe value

Do not include secrets.

### Required README Scope and Production Notes

Clearly state that this is a compact demonstration of durable retry and failure-handling patterns.

The README must not imply that IssueFlow is a production payment platform or that its SQLite/single-worker implementation is horizontally scalable.

Include a short production-evolution note describing likely changes for a larger system, such as:

- PostgreSQL or another production database.
- A durable message broker or managed queue where justified.
- Stronger job-claiming/locking semantics.
- Multiple worker instances.
- Distributed observability.
- Reconciliation workflows for ambiguous third-party outcomes.

These should be described as architectural evolution options, not requirements for this demo.

### README Quality Requirement

The README should remain hiring-manager friendly.

Do not bury the project introduction beneath implementation details.

Prefer this overall order:

1. Project purpose.
2. Architecture overview.
3. Key features.
4. Triage and workflow logic.
5. Reliable outbound notification / retry architecture.
6. API overview.
7. Running locally.
8. Testing.
9. Retry demo scenarios.
10. Configuration.
11. Scope / production considerations.

Cursor should preserve useful existing README content and integrate the new material cleanly rather than replacing unrelated documentation.

### Final README Verification

Before considering the feature complete, Cursor must verify that the root `README.md` accurately reflects the code actually implemented.

The final review must check for:

- Stale architecture diagrams.
- Incorrect endpoint names.
- Incorrect retry status codes.
- Incorrect retry timing.
- Incorrect configuration names.
- Features documented but not implemented.
- Features implemented but not documented.
- Claims of production guarantees that the implementation does not provide.

The implementation is not complete until the main README has been updated and reviewed.
