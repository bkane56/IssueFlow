---
name: implement-requirement
description: Implement an IssueFlow requirement using a disciplined inspect, plan, implement, test, and verify workflow.
---

# Implement Requirement

Use this skill when implementing a non-trivial IssueFlow feature, change, or defect fix.

## Workflow

1. Read the relevant portion of `ISSUEFLOW_CURSOR_REQUIREMENTS.md`.

2. Inspect the existing implementation before editing.

3. Determine:
   - what already exists
   - what is missing
   - which files should change
   - whether API contracts are affected
   - whether database entities or persistence are affected
   - whether frontend and backend changes must stay synchronized

4. Produce a concise implementation plan before making non-trivial changes.

5. Identify assumptions, risks, and relevant edge cases.

6. Implement the smallest complete solution that satisfies the requirement.

7. Follow all applicable `.cursor/rules/` rules.

8. Add or update meaningful tests.

9. Run focused tests first.

10. Run the broader applicable test suite.

11. Run the applicable build or verification command:
    - backend: `./mvnw clean verify`
    - frontend: `yarn build`

12. Review the final diff for:
    - unrelated changes
    - duplicated logic
    - one-off hacks
    - dead code
    - unnecessary dependencies
    - missing validation
    - missing error handling
    - missing tests
    - configuration changes not reflected in `.env.example`

13. Exercise the feature locally when practical.

14. Verify the actual requirement is satisfied rather than merely verifying that tests pass.

15. Report:
    - what changed
    - files changed
    - tests run
    - build or verification result
    - assumptions
    - remaining limitations or follow-up work

## Debugging Requirement

If the task involves a defect:

- State the root cause before applying the fix.
- Fix the root cause.
- Do not add a symptom-masking workaround unless it is independently justified.
- Add a regression test when practical.
