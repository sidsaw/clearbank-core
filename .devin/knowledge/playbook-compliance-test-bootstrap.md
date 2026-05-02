---
description: Step-by-step playbook for bootstrapping compliance test coverage on a ClearBank Core service that has no tests or incomplete tests. Run this playbook when assigned a coverage task against any service in this repo.
---

# Playbook: Compliance Test Bootstrap

Use this playbook whenever you are asked to add test coverage to a ClearBank Core service. It is designed to be repeatable across all four services (and any future services added to the monorepo). Follow every step in order.

---

## Step 1 — Check test framework setup

Inspect the service directory and determine whether a test framework is already configured.

### Java services (`auth-service`, `transaction-service`)

Check `pom.xml` for the following dependencies:

| Dependency | Expected version | Required? |
|---|---|---|
| `junit-jupiter` | 5.10.x | Always |
| `mockito-core` | 5.7.x | If collaborators need mocking |

If either is missing, add it to `<dependencies>` in `pom.xml`. Do **not** change the Java version (`17`) or the groupId (`com.clearbank`).

Also verify a `src/test/` directory exists. If it doesn't, create it.

**Current state:**
- `auth-service` — JUnit 5.10.0 configured. `src/test/` exists with one test file.
- `transaction-service` — JUnit 5.10.0 + Mockito 5.7.0 configured. `src/test/` exists but is empty (`.gitkeep` only).

### Python services (`pii-service`)

`requirements.txt` intentionally has no runtime dependencies. `pytest` is the test framework — it must be available in the CI environment but is not listed in `requirements.txt`. No `setup.cfg` or `pyproject.toml` is needed.

Verify a `src/test/` or `tests/` directory exists. If it doesn't, create `tests/` at the service root.

**Current state:**
- `pii-service` — No test directory, no test files.

### TypeScript services (`audit-service`)

Check `package.json` for `vitest` in `devDependencies`. If missing, add it:

```json
"devDependencies": {
  "typescript": "^5.3.0",
  "vitest": "^1.0.0"
}
```

Then add a `test` script:

```json
"scripts": {
  "test": "vitest run"
}
```

Run `npm install` after editing `package.json`.

**Current state:**
- `audit-service` — only `typescript` in devDependencies. Vitest is **not** configured. Must be added before any tests can run.

---

## Step 2 — Identify compliance-critical functions

For each service, the following functions are compliance-critical and **must** have test coverage. These are the minimum — cover additional functions if time permits.

### auth-service (`src/auth.java` → `AuthService`)

| Function | Why compliance-critical |
|---|---|
| `login(username, password)` | Credential validation — must reject bad passwords and unknown users |
| `validateToken(token)` | Token integrity check — gates all downstream service access |
| `logout(token)` | Session termination — must not allow reuse after logout |

**Known gap:** `logout` currently just prints to stdout and does not invalidate the token. Note this in the audit report but do not refactor the implementation — test the observable behaviour as-is and flag the gap as a finding.

### transaction-service (`src/transactions.java` → `TransactionService`)

| Function | Why compliance-critical |
|---|---|
| `deposit(accountId, amount)` | Ledger mutation — incorrect balance = financial misstatement |
| `withdraw(accountId, amount)` | Ledger mutation + overdraft prevention |
| `transfer(fromId, toId, amount)` | Atomic ledger mutation across two accounts |
| `getBalance(accountId)` | Balance read — must reject unknown accounts |

### pii-service (`src/pii.py`)

| Function | Why compliance-critical |
|---|---|
| `mask_ssn(ssn)` | SSN exposure — must mask correctly and reject malformed input |
| `mask_account(account_number)` | Account number exposure |
| `is_valid_email(email)` | Input validation gate |
| `redact_record(record)` | End-to-end PII pipeline — must mask all fields correctly |

### audit-service (`src/audit.ts`)

| Function | Why compliance-critical |
|---|---|
| `logEvent(eventType, userId, details)` | Audit trail integrity — every compliance event must be recorded |
| `getEvents(userId)` | Audit retrieval — must return only the correct user's events |
| `getEventsByType(eventType)` | Compliance query — must filter correctly |
| `clearEvents()` | Test utility — must actually empty the store |

---

## Step 3 — Write unit tests

Apply the `!mocking-libraries` and `!pii-in-tests` knowledge macros before writing any tests.

### General rules

- One test file per service source file. Name it after the class/module under test:
  - Java: `AuthTest.java`, `TransactionServiceTest.java`
  - Python: `test_pii.py`
  - TypeScript: `audit.test.ts`
- Every compliance-critical function from Step 2 must have tests for **all** of the following case categories:

| Category | Examples |
|---|---|
| Happy path | Valid inputs, expected return values |
| Null / missing input | `null`, `None`, `undefined`, empty string |
| Malformed input | Wrong format, wrong type |
| Boundary values | Zero amounts, single-character strings, minimum/maximum |
| Auth / access failures | Wrong password, unknown user, invalid token |
| Financial edge cases | Insufficient funds, same-account transfer, zero-amount operations |

### PII rule (mandatory)
Follow the `!pii-in-tests` knowledge. Assert on masked output only. Never print or log raw PII. Use obviously-fake synthetic values (`123-45-6789`, `ACC001`, `user@example.com`).

### Java test conventions
```java
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService();
    }

    @Test
    void deposit_increasesBalance() { ... }

    @Test
    void withdraw_throwsOnInsufficientFunds() {
        assertThrows(IllegalStateException.class,
            () -> service.withdraw("ACC001", 99999.00));
    }
}
```

### Python test conventions
```python
import pytest
from src.pii import mask_ssn, redact_record

def test_mask_ssn_returns_masked_form():
    assert mask_ssn("123-45-6789") == "XXX-XX-6789"

def test_mask_ssn_raises_on_invalid_format():
    with pytest.raises(ValueError):
        mask_ssn("not-an-ssn")
```

### TypeScript test conventions
```typescript
import { describe, it, expect, beforeEach } from 'vitest';
import { logEvent, getEvents, clearEvents } from '../src/audit';

describe('audit', () => {
  beforeEach(() => clearEvents());

  it('logEvent records an event retrievable by userId', () => {
    logEvent('LOGIN', 'user1', {});
    expect(getEvents('user1')).toHaveLength(1);
  });
});
```

---

## Step 4 — Verify CI runs tests

Check `.github/workflows/` for a workflow that actually **runs** tests for the service (not just the coverage estimation script).

**Current state:** The only CI workflows are:
- `coverage-report.yml` — runs `python3 coverage_report.py` (a line-count estimate, not real test execution)
- `devin-trigger.yml` — Devin session launcher

Neither workflow runs `mvn test`, `pytest`, or `vitest`. This means **no tests are currently executed in CI**.

If no test-execution workflow exists, create `.github/workflows/test.yml` with jobs for each service:

```yaml
name: Tests

on:
  pull_request:
    types: [opened, synchronize]
  push:
    branches: [main]

jobs:
  auth-service:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: mvn test
        working-directory: auth-service

  transaction-service:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: mvn test
        working-directory: transaction-service

  pii-service:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - run: pip install pytest
      - run: pytest
        working-directory: pii-service

  audit-service:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm install
        working-directory: audit-service
      - run: npm test
        working-directory: audit-service
```

---

## Step 5 — Generate coverage audit report

After all tests are written and the CI workflow passes:

1. Run `python3 coverage_report.py` from the repo root to get updated coverage numbers.
2. Create a new file in `audit_reports/` named `coverage-audit-PR-<PR_NUMBER>.md`.
3. The report must follow the standard audit report format (see the devin-trigger prompt for the template).
4. Include a **Compliance Findings** section listing any gaps discovered during Step 2 (e.g. the `logout` non-invalidation issue in auth-service).
5. Commit the report to the PR branch.

---

## Checklist

Before marking the task complete, verify every item:

- [ ] Test framework configured in `pom.xml` / `package.json` / CI environment
- [ ] Test file exists for every service touched
- [ ] Every compliance-critical function from Step 2 has tests
- [ ] All four case categories covered: happy path, null/malformed input, boundary values, failure paths
- [ ] No raw PII in assertions, log output, or test names
- [ ] `.github/workflows/test.yml` exists and all jobs pass
- [ ] `audit_reports/coverage-audit-PR-<N>.md` created and committed
