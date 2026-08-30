# IssueFlow Project Requirements

## 1. Project Overview

IssueFlow is a compact full-stack incident and support issue triage application intended to demonstrate professional React and Java/Spring Boot engineering practices in a realistic interview demo.

The application must allow users to create, review, prioritize, assign, update, and resolve software incidents or operational support issues.

The project should be polished enough to demonstrate during a technical interview while remaining intentionally small enough to credibly complete over a weekend.

Primary goals:

- Demonstrate a modern React frontend.
- Demonstrate a Java 17 Spring Boot REST service.
- Demonstrate relational persistence using SQLite.
- Demonstrate meaningful backend business logic beyond basic CRUD.
- Demonstrate clean architecture, validation, testing, error handling, and API documentation.
- Provide useful seeded demo data so the application is immediately presentable after startup.
- Keep the architecture simple and appropriate for a single-machine demo application.

Do not over-engineer this project.

---

# 2. Required Technology Stack

## 2.1 Frontend

Use:

- React
- TypeScript
- Vite
- React Router
- Yarn for all JavaScript and TypeScript package management
- Vitest
- React Testing Library

### Package Management Requirement

Yarn is mandatory.

Use:

```bash
yarn
yarn add <package>
yarn dev
yarn test
yarn build
```

Do not use:

```bash
npm
npm install
npm run
npx
pnpm
```

Do not generate `package-lock.json`.

The repository should contain `yarn.lock`.

---

## 2.2 Backend

Use:

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Jakarta Bean Validation
- SQLite
- Maven
- JUnit 5
- Mockito
- Spring Boot Test
- springdoc-openapi / Swagger UI

### Package Management Requirement

Maven is mandatory.

Use the Maven Wrapper whenever practical.

Preferred commands:

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean verify
./mvnw package
```

Do not use:

- Gradle
- Gradle Wrapper
- Ant

The backend must contain:

```text
pom.xml
mvnw
mvnw.cmd
.mvn/
```

---

# 3. Repository Structure

Use a simple monorepo structure.

```text
issueflow/
├── README.md
├── .gitignore
├── .env.example
├── frontend/
│   ├── package.json
│   ├── yarn.lock
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
└── backend/
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    ├── .mvn/
    └── src/
```

Do not introduce workspaces, Nx, Turborepo, Docker Compose, Kubernetes, or additional repository tooling unless a concrete requirement makes it necessary.

---

# 4. Backend Architecture

Use conventional layered Spring Boot architecture.

Suggested package structure:

```text
com.issueflow
├── IssueFlowApplication.java
├── config/
├── constants/
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── entity/
├── exception/
├── mapper/
├── repository/
├── service/
└── validation/
```

Responsibilities must be clearly separated.

## Controller Layer

Controllers must:

- Accept HTTP requests.
- Validate request DTOs.
- Delegate business operations to services.
- Return response DTOs.
- Avoid containing business logic.

## Service Layer

Services must:

- Implement application and domain business rules.
- Coordinate repositories.
- Perform triage calculations.
- Manage issue status transitions.
- Create audit/history records when applicable.

## Repository Layer

Repositories must:

- Handle database persistence.
- Use Spring Data JPA.
- Avoid business logic.

## DTOs

Do not expose JPA entities directly through REST endpoints.

Use request and response DTOs.

## Mapper

Mapping between entities and DTOs should be explicit and easy to understand.

Do not add a mapping framework unless it provides a clear benefit for this small project.

---

# 5. Constants

Do not scatter constant values or magic strings throughout the application.

Backend constants should live under:

```text
backend/src/main/java/com/issueflow/constants/
```

Organize constants by responsibility rather than putting every value into one enormous file.

Examples:

```text
constants/
├── ApiConstants.java
├── TriageConstants.java
├── ValidationConstants.java
└── ErrorConstants.java
```

Examples of values that should be constants where appropriate:

- API path prefixes
- Triage score thresholds
- Triage score weights
- Validation limits
- Standard error messages
- Seed-data-specific reusable values when appropriate

Prefer enums instead of strings when representing closed domain concepts such as status, severity, priority, or category.

Frontend constants should be placed under:

```text
frontend/src/constants/
```

Do not duplicate shared values unnecessarily throughout UI components.

---

# 6. Domain Model

The primary domain objects are:

- Issue
- User
- IssueHistory

## 6.1 Issue

An Issue should contain at minimum:

```text
id
title
description
category
severity
priority
priorityScore
status
assignedUser
customerFacing
productionImpact
affectedUsers
createdAt
updatedAt
```

Recommended enums:

### IssueStatus

```text
NEW
TRIAGED
IN_PROGRESS
RESOLVED
CLOSED
```

### Severity

```text
LOW
MEDIUM
HIGH
CRITICAL
```

### Priority

```text
P1
P2
P3
P4
```

### Category

Suggested initial values:

```text
FRONTEND
BACKEND
DATABASE
INFRASTRUCTURE
SECURITY
INTEGRATION
OTHER
```

## 6.2 User

A User should contain:

```text
id
name
email
active
```

Authentication is not required for the initial version.

Users primarily represent assignable team members.

## 6.3 IssueHistory

IssueHistory provides an audit trail.

Minimum fields:

```text
id
issueId
eventType
oldValue
newValue
description
createdAt
```

Examples:

```text
ISSUE_CREATED
STATUS_CHANGED
PRIORITY_CHANGED
ASSIGNEE_CHANGED
TRIAGE_RECALCULATED
ISSUE_UPDATED
```

---

# 7. SQLite Persistence

Use SQLite as the relational database.

The database should be stored locally in a predictable development location such as:

```text
backend/data/issueflow.db
```

Do not commit the runtime SQLite database file to Git.

Add the database path to `.gitignore`.

JPA/Hibernate may manage the schema for this demo project.

The configuration must make it easy for an interviewer or developer to clone the repository and start the application locally.

---

# 8. Triage Engine

The triage engine is the primary business feature distinguishing IssueFlow from a generic CRUD application.

Implement it as a dedicated backend service.

Suggested class:

```text
TriageService
```

The triage engine calculates a numerical priority score using issue characteristics.

Initial scoring rules:

| Condition | Score |
|---|---:|
| Production impact | +50 |
| Critical severity | +40 |
| High severity | +25 |
| Medium severity | +10 |
| Customer facing | +20 |
| 100 or more affected users | +20 |
| 25 to 99 affected users | +10 |
| Issue older than 24 hours and unresolved | +10 |

Priority thresholds:

| Score | Priority |
|---|---|
| 90 or greater | P1 |
| 60 to 89 | P2 |
| 30 to 59 | P3 |
| 0 to 29 | P4 |

These values must be stored as named constants and must not be scattered as magic numbers throughout the code.

The service should return both:

- Calculated score
- Explanation of how the score was derived

Example:

```json
{
  "score": 110,
  "priority": "P1",
  "factors": [
    {
      "name": "Production impact",
      "score": 50
    },
    {
      "name": "Critical severity",
      "score": 40
    },
    {
      "name": "Customer facing",
      "score": 20
    }
  ]
}
```

The explanation must be available to the frontend.

This makes the business rule transparent and demonstrable.

---

# 9. Issue Lifecycle

Supported lifecycle:

```text
NEW
  |
  v
TRIAGED
  |
  v
IN_PROGRESS
  |
  v
RESOLVED
  |
  v
CLOSED
```

For the initial version, do not build an elaborate workflow engine.

The service should prevent obviously invalid transitions where reasonable.

Examples:

- CLOSED should not transition directly back to IN_PROGRESS without an intentional reopen capability.
- Creating an issue should automatically record an ISSUE_CREATED history entry.
- Changing status should create a STATUS_CHANGED history entry.
- Recalculating priority should create history when the priority changes.

---

# 10. REST API

Base path:

```text
/api
```

## Issues

### List Issues

```http
GET /api/issues
```

Support optional filtering for:

```text
status
priority
severity
category
assignedUserId
search
```

Search should match at minimum:

- title
- description

### Get Issue

```http
GET /api/issues/{id}
```

### Create Issue

```http
POST /api/issues
```

Creating an issue should automatically run triage.

### Update Issue

```http
PUT /api/issues/{id}
```

### Delete Issue

```http
DELETE /api/issues/{id}
```

Deletion can be supported for the demo but should not be emphasized in the UI.

### Change Status

```http
PATCH /api/issues/{id}/status
```

### Assign Issue

```http
PATCH /api/issues/{id}/assign
```

### Recalculate Triage

```http
POST /api/issues/{id}/triage
```

### Issue History

```http
GET /api/issues/{id}/history
```

---

# 11. Dashboard API

Provide:

```http
GET /api/dashboard
```

The backend should calculate dashboard values.

Example response:

```json
{
  "open": 12,
  "critical": 2,
  "inProgress": 5,
  "resolved": 18
}
```

Do not require the frontend to retrieve all issues merely to calculate dashboard statistics.

Additional useful statistics may be added if simple to implement, but the endpoint should remain concise.

---

# 12. Users API

Provide:

```http
GET /api/users
GET /api/users/{id}
```

User creation and management screens are not required for the initial version.

Seed several users for assignment purposes.

---

# 13. Validation

Use Jakarta Bean Validation for backend request validation.

Examples:

- Title is required.
- Title should have a reasonable maximum length.
- Description is required.
- Severity is required.
- Category is required.
- affectedUsers cannot be negative.
- Email addresses must have a valid format where relevant.

Do not rely solely on frontend validation.

Return useful validation errors to the frontend.

---

# 14. Error Handling

Implement centralized backend exception handling using:

```text
@RestControllerAdvice
```

Provide consistent JSON error responses.

Suggested format:

```json
{
  "timestamp": "2026-08-30T13:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Issue 1042 was not found",
  "path": "/api/issues/1042"
}
```

At minimum handle:

- Entity not found
- Validation errors
- Invalid enum/input values
- Invalid state transitions
- Unexpected server errors

Do not expose Java stack traces to the frontend.

Do not silently swallow exceptions.

---

# 15. Frontend Architecture

Suggested structure:

```text
frontend/src/
├── api/
├── components/
├── constants/
├── hooks/
├── layouts/
├── pages/
├── types/
├── utils/
├── App.tsx
└── main.tsx
```

Keep components reasonably small and focused.

Do not introduce Redux unless shared state becomes complex enough to justify it.

For this project, server state can be handled using normal React patterns or a lightweight query library if needed.

Avoid unnecessary global state.

---

# 16. Required Frontend Pages

## 16.1 Dashboard

Route:

```text
/
```

Display summary cards for:

- Open Issues
- Critical Issues
- In Progress
- Resolved

Below the cards, display useful recent or high-priority issues.

The page should look populated immediately using seeded backend data.

## 16.2 Issues List

Route:

```text
/issues
```

Display issues in a table.

Recommended columns:

```text
ID
Title
Category
Severity
Priority
Status
Assignee
Updated
```

Support:

- Text search
- Status filter
- Priority filter
- Severity filter
- Category filter
- Assignee filter
- Clear filters

The issue title should navigate to the issue detail page.

## 16.3 Issue Detail

Route:

```text
/issues/:id
```

Display:

- Issue title
- Description
- Category
- Severity
- Priority
- Status
- Assignee
- Customer-facing flag
- Production-impact flag
- Affected-user count
- Created timestamp
- Updated timestamp

Include a Triage Explanation section.

Example:

```text
Production impact          +50
Critical severity          +40
Customer facing            +20
--------------------------------
Priority score             110

Assigned priority          P1
```

Include:

- Change status capability
- Change assignee capability
- Recalculate Priority button
- Issue history timeline

When triage causes a priority change, clearly show the change.

Example:

```text
P3 -> P1
```

## 16.4 Create Issue

Route:

```text
/issues/new
```

Provide a clean form for creating an issue.

The backend must calculate priority.

Do not allow the user to manually select the calculated priority during normal issue creation.

## 16.5 Edit Issue

Route:

```text
/issues/:id/edit
```

Allow appropriate issue fields to be updated.

---

# 17. UI Requirements

The UI should be professional and suitable for an interview demonstration.

Use a clean application layout containing:

- Header
- Application name
- Navigation
- Main content area

Recommended navigation:

```text
Dashboard
Issues
New Issue
API Docs
```

Use visual badges for:

- Priority
- Severity
- Status

Color may help distinguish severity and priority but must not be the only way information is communicated.

The interface should work well on a normal laptop display.

Mobile-perfect responsive design is not required, but the application should not break badly on narrower screens.

Avoid excessive animation.

Avoid visual clutter.

Do not use emojis in the user interface or source code.

Do not use em dashes in source code, comments, documentation, or generated project text.

---

# 18. Seed Data

The application must start with useful demo data.

Seed approximately:

- 5 users
- 15 to 25 issues
- Associated issue history records

Include a realistic mix of:

- P1 through P4 issues
- Open and resolved issues
- Frontend issues
- Backend issues
- Database issues
- Infrastructure issues
- Customer-facing incidents
- Production incidents

Example issue titles:

```text
Checkout API returning intermittent 500 responses
Customer dashboard fails to load account history
Nightly billing reconciliation job exceeded SLA
Search results occasionally return stale product data
Database connection pool saturation during peak traffic
Password reset email delivery delayed
```

Seed data must be deterministic enough that the application looks consistent during an interview.

Avoid joke data and placeholder strings such as:

```text
foo
bar
test test
lorem ipsum
```

---

# 19. Swagger / OpenAPI

Expose Swagger UI using springdoc-openapi.

Document the REST endpoints.

Swagger should be accessible locally through the normal springdoc path, typically:

```text
http://localhost:8080/swagger-ui/index.html
```

The README should contain the exact working URL.

The frontend may contain an API Docs navigation link that opens Swagger UI.

---

# 20. Environment Variables and Configuration

Do not hard-code environment-specific configuration that reasonably belongs in configuration.

Maintain a root:

```text
.env.example
```

and/or project-specific example environment files if needed.

Any environment variable added during development must also be added to the relevant example file.

Example values must be sanitized.

For values that would normally contain secrets, use a partial example followed by asterisks when useful.

Example:

```text
SOME_API_KEY=abc12*******
```

No actual secrets may be committed.

For this project, expected configuration may include:

```text
VITE_API_BASE_URL=http://localhost:8080
```

SQLite location may remain in Spring configuration unless there is a reason to make it configurable.

---

# 21. Testing Requirements

Testing should focus on meaningful behavior rather than artificially maximizing coverage.

## Backend Tests

At minimum test:

### TriageService

Test:

- P1 calculation
- P2 calculation
- P3 calculation
- P4 calculation
- Production impact scoring
- Customer-facing scoring
- Severity scoring
- Affected-user thresholds
- Age-based scoring
- Triage explanation
- Boundary values around priority thresholds

### IssueService

Test:

- Issue creation
- Automatic triage
- Issue updates
- Status changes
- Assignment
- History creation
- Recalculation of priority

### Controllers

Test important REST behavior including:

- Successful requests
- Validation failures
- Not-found responses

## Frontend Tests

At minimum test:

- Dashboard statistics render
- Issues table renders
- Filtering behavior
- Create issue form validation
- Issue detail rendering
- Triage explanation rendering
- Priority recalculation result rendering

Do not write trivial tests solely to increase the number of tests.

When fixing a defect, add a regression test when practical.

---

# 22. README Requirements

The root README should be written for an engineer or interviewer seeing the repository for the first time.

Include:

## Project Summary

Explain what IssueFlow does.

## Why It Exists

State that the project demonstrates a compact full-stack architecture using React, Java/Spring Boot, and SQLite.

Do not oversell the project as a production enterprise platform.

## Architecture

Include a simple diagram such as:

```text
React / TypeScript
       |
       | REST / JSON
       v
Spring Boot
       |
       | JPA
       v
SQLite
```

## Technology Stack

Clearly list:

- React
- TypeScript
- Vite
- Java 17
- Spring Boot
- Maven
- SQLite
- JPA/Hibernate
- JUnit
- Vitest
- Swagger/OpenAPI

## Running Locally

Frontend:

```bash
cd frontend
yarn
yarn dev
```

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

## Running Tests

Frontend:

```bash
yarn test
```

Backend:

```bash
./mvnw test
```

## API Documentation

Provide the Swagger URL.

## Business Logic

Explain the triage scoring model and why it exists.

## Screenshots

Add screenshots once the UI is complete if practical.

---

# 23. Git Requirements

Create an appropriate `.gitignore`.

At minimum exclude:

```text
node_modules/
dist/
target/
.idea/
.vscode/
.DS_Store
*.db
*.sqlite
*.sqlite3
.env
```

Do commit:

```text
.env.example
yarn.lock
pom.xml
mvnw
mvnw.cmd
.mvn/
```

Do not commit generated build artifacts.

---

# 24. Security

This is a local interview demonstration and does not require authentication in the initial release.

However:

- Validate all request input.
- Do not expose stack traces through REST responses.
- Do not hard-code credentials.
- Do not commit secrets.
- Avoid unsafe SQL construction.
- Use repository/JPA parameterization.
- Avoid logging unnecessary request data.

Do not add OAuth2, JWT, Spring Security, or user registration unless specifically requested later.

The absence of authentication is intentional scope control for the weekend demo.

---

# 25. Non-Goals

The initial project does not require:

- Authentication
- OAuth2
- User registration
- AWS deployment
- Kubernetes
- Kafka
- Redis
- Microservices
- WebSockets
- Email notifications
- LLM integration
- Mobile application
- Complex role-based permissions
- Event sourcing
- CQRS
- Distributed caching
- Elasticsearch
- Production observability stack

Do not introduce these features unless requirements change.

---

# 26. Implementation Order

Cursor should implement the project incrementally.

Recommended order:

1. Create repository structure.
2. Create Spring Boot backend.
3. Configure SQLite.
4. Implement entities and repositories.
5. Implement DTOs and validation.
6. Implement TriageService.
7. Implement IssueService.
8. Implement REST controllers.
9. Implement centralized error handling.
10. Add seed data.
11. Add Swagger/OpenAPI.
12. Add backend tests.
13. Create React/Vite/TypeScript frontend using Yarn.
14. Implement API client.
15. Implement application layout and routing.
16. Implement dashboard.
17. Implement issue list and filtering.
18. Implement issue detail.
19. Implement create/edit issue forms.
20. Implement triage explanation and recalculation UI.
21. Implement history timeline.
22. Add frontend tests.
23. Complete README.
24. Run backend verification.
25. Run frontend verification.
26. Review the complete diff and remove unnecessary code.

Do not attempt to implement the entire application in one enormous change.

---

# 27. Cursor / AI-Assisted Engineering Rules

Before implementing non-trivial changes:

- Read the relevant requirements.
- Inspect the existing code.
- Produce a concise implementation plan.
- Identify files expected to change.
- Call out assumptions or risks.

During implementation:

- Implement the actual requirement rather than merely making tests pass.
- Keep business logic explicit.
- Prefer the smallest reasonable solution.
- Do not introduce unnecessary abstractions.
- Do not alter unrelated code.
- Reuse existing abstractions where appropriate.
- Validate input at appropriate boundaries.
- Handle errors at the correct architectural layer.

When debugging:

- Identify the root cause.
- Fix the root cause.
- Do not add one-off conditionals, retries, fallbacks, or exception swallowing merely to hide a defect.
- Do not weaken a correct test simply to make the test suite pass.

Before declaring a task complete:

- Review the diff.
- Confirm every changed file is necessary.
- Run relevant tests.
- Run the broader applicable test suite.
- Run or exercise the feature locally when practical.
- Verify normal cases and important failure cases.
- Confirm the implementation matches this requirements file.
- Document assumptions or limitations.

---

# 28. Definition of Done

The initial project is complete when:

- The React application starts using Yarn.
- The Spring Boot application starts using Maven.
- SQLite initializes successfully.
- Seed data loads.
- The dashboard displays meaningful statistics.
- Issues can be listed and filtered.
- An issue can be created.
- An issue can be edited.
- An issue can be assigned.
- Issue status can be changed.
- Triage priority is calculated by the backend.
- Triage reasoning is visible in the UI.
- Triage can be recalculated.
- Priority changes are recorded in issue history.
- Issue history is visible.
- Validation errors are handled cleanly.
- Swagger documents the API.
- Meaningful backend tests pass.
- Meaningful frontend tests pass.
- `yarn build` succeeds.
- `./mvnw clean verify` succeeds.
- The README contains correct setup and demo instructions.
- No secrets or local SQLite database files are committed.
- The application can be demonstrated locally without requiring external services.

---

# Guiding Principle

IssueFlow should demonstrate disciplined full-stack engineering, not architectural complexity.

A reviewer should be able to quickly understand:

- What the application does.
- How React communicates with Spring Boot.
- Where business logic lives.
- How data is persisted.
- How triage priority is calculated.
- How errors and validation are handled.
- How the implementation is tested.
- How to run the project locally.

Keep it clean, explainable, tested, and finished.
