# IssueFlow

IssueFlow is a compact incident and support issue triage application. It helps a team create, review, prioritize, assign, update, and resolve software incidents.

## Why it exists

This project demonstrates a compact full-stack architecture using React, Java 17 / Spring Boot, and SQLite. It is designed as an interview demo: small enough to finish in a weekend, but complete enough to show validation, testing, API documentation, and meaningful business logic.

It is not a production enterprise platform. Authentication, cloud deployment, and extra infrastructure were intentionally left out.

## Architecture

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

- The React UI talks to a single REST API.
- Controllers validate requests and return DTOs.
- Services own issue lifecycle, assignment, and history.
- `TriageService` calculates priority scores and explanations.
- SQLite stores users, issues, and history locally.

## Technology stack

- React
- TypeScript
- Vite
- Java 17
- Spring Boot
- Maven
- SQLite
- JPA / Hibernate
- JUnit
- Vitest
- Swagger / OpenAPI

## Running locally

The application does not require external services.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`.

SQLite is created at `backend/data/issueflow.db` on first start. Seed data loads automatically when the database is empty: 5 users and 20 issues.

### Frontend

```bash
cd frontend
yarn
yarn dev
```

The UI is available at `http://localhost:3000`.

Optional environment variable:

```text
VITE_API_BASE_URL=http://localhost:8080
```

Copy `.env.example` or `frontend/.env.example` if you want a local file. The frontend defaults to `http://localhost:8080` when the variable is not set.

## Running tests

Frontend:

```bash
cd frontend
yarn test
```

Backend:

```bash
cd backend
./mvnw test
```

Full backend verification:

```bash
cd backend
./mvnw clean verify
```

Frontend production build:

```bash
cd frontend
yarn build
```

## API documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

The frontend navigation includes an API Docs link to that URL.

## Triage business logic

Priority is never chosen by the user during normal create or edit flows. The backend calculates a score from issue characteristics:

| Condition | Score |
|---|---:|
| Production impact | +50 |
| Critical severity | +40 |
| High severity | +25 |
| Medium severity | +10 |
| Customer facing | +20 |
| 100 or more affected users | +20 |
| 25 to 99 affected users | +10 |
| Older than 24 hours and unresolved | +10 |

Priority bands:

| Score | Priority |
|---|---|
| 90 or greater | P1 |
| 60 to 89 | P2 |
| 30 to 59 | P3 |
| 0 to 29 | P4 |

The API returns both the score and the contributing factors so the UI can show why a priority was assigned. Recalculating triage writes history when the priority changes.

Issue status follows a simple forward path:

```text
NEW -> TRIAGED -> IN_PROGRESS -> RESOLVED -> CLOSED
```

Invalid jumps, such as moving a closed issue back to in progress, are rejected.

## SQLite behavior

- The runtime database file is local and gitignored.
- Hibernate updates the schema on startup.
- Restarting the application reuses existing data and does not duplicate seed records.
- Cloning the repository and starting the backend is enough to get a populated demo.

## Intentional scope limits

The first version does not include authentication, user registration, email notifications, WebSockets, Redis, Docker, or cloud deployment. Those omissions keep the demo explainable.

## Project layout

```text
issueflow/
├── README.md
├── .gitignore
├── .env.example
├── frontend/
└── backend/
```
