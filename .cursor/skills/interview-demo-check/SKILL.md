---
name: interview-demo-check
description: Perform a final IssueFlow interview demo readiness review across build, tests, functionality, configuration, and repository quality.
---

# Interview Demo Check

Use this skill before showing IssueFlow to an interviewer.

Treat the repository as if a senior engineer is about to inspect and run it.

Do not modify code during the initial review unless explicitly instructed. First identify readiness issues and report them clearly.

## 1. Repository Review

Check:

- repository structure is understandable
- no unexpected generated files are committed
- no `package-lock.json`
- `yarn.lock` exists
- Maven Wrapper exists
- `.gitignore` excludes build artifacts, `.env`, and SQLite runtime files
- `.env.example` reflects current environment variables
- no secrets are present
- no unnecessary dependencies or infrastructure were introduced

## 2. Backend Verification

Run:

```bash
cd backend
./mvnw clean verify
```

Verify:

- Spring Boot application starts successfully
- SQLite initializes successfully
- seed data loads
- dashboard endpoint responds
- issue list endpoint responds
- issue detail endpoint responds
- issue creation works
- validation failures return useful errors
- status changes work
- assignment works
- triage recalculation works
- issue history works
- not-found behavior is clean
- Swagger UI is reachable

## 3. Frontend Verification

Run:

```bash
cd frontend
yarn test
yarn build
```

Verify the application can demonstrate:

- populated dashboard
- issue list
- text search
- filters
- issue detail
- create issue
- edit issue
- assignee change
- status change
- triage explanation
- triage recalculation
- priority change display
- history timeline
- useful validation messages
- useful error states

## 4. Demo Data Review

Verify seeded data:

- looks realistic
- includes multiple severities and priorities
- includes open and resolved issues
- includes multiple categories
- includes production and customer-facing incidents
- contains enough records to make filters and dashboard statistics meaningful
- does not contain joke or placeholder data

## 5. README Review

Verify the README clearly explains:

- what IssueFlow does
- why it exists
- architecture
- technology stack
- backend startup
- frontend startup
- testing commands
- Swagger URL
- triage business logic
- SQLite behavior
- known intentional scope limitations

Verify every documented command actually works.

## 6. Code Review

Inspect for:

- business logic in controllers
- repository calls directly from controllers
- duplicated triage logic
- magic numbers
- oversized React components
- API calls scattered through presentation components
- unnecessary global state
- swallowed exceptions
- debug logging
- TODO items that undermine the demo
- commented-out code
- dead code
- unused dependencies
- em dash characters
- emojis

## 7. Final Readiness Report

Produce a concise report in this format:

```text
INTERVIEW DEMO READINESS

PASS <check>
PASS <check>
WARN <check>
FAIL <check>

Demo readiness: READY | READY WITH MINOR WARNINGS | NOT READY

Recommended fixes:
1. <highest priority fix>
2. <next fix>
3. <next fix>
```

Prioritize actual demo blockers over cosmetic improvements.

Do not recommend adding new architecture or features merely to make the project look more sophisticated.
