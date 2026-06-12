# AI-Assisted Testing and Self-Healing

This project now includes AI-inspired testing utilities for frontend and backend.

## What was added

### Frontend
- `frontend/e2e/utils/selfHealingLocators.js`
  - Adaptive locator fallback for Playwright E2E tests.
- `frontend/src/test/mocks/selfHealingQuery.ts`
  - Fuzzy role query fallback for unit/component tests.
- `frontend/src/test/mocks/selfHealingQuery.test.tsx`
  - Harness tests for self-healing query behavior.
- `frontend/tools/ai-test-healer.mjs`
  - Failure-log analyzer with actionable suggestions.
- `frontend/playwright.config.mjs`
  - Optional retry in self-heal mode (`E2E_SELF_HEAL=true`).
- `frontend/package.json`
  - Added scripts:
    - `test:e2e:self-heal`
    - `test:ai:analyze`

### Backend
- `backend/src/test/java/com/certifreight/backend/testsupport/AiShipmentRequestCases.java`
  - Shared AI-generated request scenario catalog.
- `backend/src/test/java/com/certifreight/backend/model/AiGeneratedShipmentRequestValidationTest.java`
  - Unit-level boundary validation driven by generated cases.
- `backend/src/test/java/com/certifreight/backend/integration/AiGeneratedShipmentApiIntegrationTest.java`
  - Integration-level API verification driven by generated cases.
- `backend/tools/ai-test-healer.mjs`
  - Surefire report analyzer with repair hints.
- `backend/pom.xml`
  - Enabled self-healing test rerun:
    - `maven-surefire-plugin` -> `rerunFailingTestsCount=1`

## Quick Try

### Frontend self-healing tests
```powershell
cd C:\Users\Sean\Documents\Code_Projects\CertiFreight\frontend
npm run test -- src/test/mocks/selfHealingQuery.test.tsx
npm run test:e2e:self-heal
npm run test:ai:analyze
```

### Backend self-healing tests
```powershell
cd C:\Users\Sean\Documents\Code_Projects\CertiFreight\backend
.\mvnw.cmd test -Dtest=AiGeneratedShipmentRequestValidationTest
.\mvnw.cmd test -Dtest=AiGeneratedShipmentApiIntegrationTest
node .\tools\ai-test-healer.mjs
```

## Notes
- Frontend self-healing is selector-focused (adaptive locators + fuzzy query fallback).
- Backend self-healing is stability-focused (automatic rerun for transient failures + generated boundary scenarios).
- AI analyzers are deterministic heuristics so they work offline and in CI without API keys.

