---
description: Step-by-step playbook for bootstrapping compliance test coverage on a ClearBank Core service that has no tests or incomplete tests. Run this playbook when assigned a coverage task against any service in this repo.
---

# Playbook: Compliance Test Bootstrap

Use this playbook whenever you are asked to add test coverage to a ClearBank Core service. It is designed to be repeatable across all services in the monorepo. Follow every step in order.

---

## Step 1 — Check test framework setup

Inspect the service directory and determine whether a test framework is already configured. Set it up if not.

### Java services

Check `pom.xml` for the following dependencies:

| Dependency | Expected version | Required? |
|---|---|---|
| `junit-jupiter` | 5.10.x | Always |
| `mockito-core` | 5.7.x | If collaborators need mocking |

If either is missing, add it to `<dependencies>` in `pom.xml`. Do **not** change the Java version (`17`) or the groupId (`com.clearbank`).

Verify a `src/test/java/` directory exists mirroring the package structure of `src/main/java/`. If it doesn't, create it.

### Python services

`pytest` is the test framework — it must be available in the CI environment but does not need to be listed in `requirements.txt`.

Verify a `tests/` directory exists at the service root. If it doesn't, create it with an empty `__init__.py`.

### TypeScript services

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

Verify a `tests/` (or `src/__tests__/`) directory exists. If it doesn't, create it.

---

## Step 2 — Identify compliance-critical functions

Read the service's source files and identify which functions are compliance-critical. These are functions that:

- Validate credentials or access tokens
- Mutate financial state (balances, ledger entries)
- Handle or transform PII
- Write to the audit trail
- Enforce business rules (rate limits, lockouts, permission checks)

Every compliance-critical function **must** have test coverage. Document the list before writing any tests.

---

## Step 3 — Write unit tests

Apply the `!mocking-libraries` and `!pii-in-tests` knowledge macros before writing any tests.

### Folder structure and naming conventions

- One test file per source file. Name it after the class or module under test:
  - Java: `<ClassName>Test.java`, placed in `src/test/java/` under the matching package path
  - Python: `test_<module_name>.py`, placed in `tests/`
  - TypeScript: `<module_name>.test.ts`, placed in `tests/`

### Case categories

Every compliance-critical function must have tests covering **all** of the following categories:

| Category | Examples |
|---|---|
| Happy path | Valid inputs, expected return values |
| Null / missing input | `null`, `None`, `undefined`, empty string |
| Malformed input | Wrong format, wrong type |
| Boundary values | Zero amounts, single-character strings, minimum/maximum |
| Failure paths | Rejected inputs, thrown exceptions, error states |

### PII rule (mandatory)

Follow the `!pii-in-tests` knowledge. Assert on masked output only. Never print or log raw PII. Use obviously-fake synthetic values (e.g. `123-45-6789`, `ACC001`, `user@example.com`).

### Java test conventions

```java
@ExtendWith(MockitoExtension.class)
class ExampleServiceTest {
    private ExampleService service;

    @BeforeEach
    void setUp() {
        service = new ExampleService();
    }

    @Test
    void methodName_descriptionOfBehaviour() { ... }

    @Test
    void methodName_throwsOnInvalidInput() {
        assertThrows(IllegalArgumentException.class,
            () -> service.methodName(invalidInput));
    }
}
```

### Python test conventions

```python
import pytest
from src.<module> import <function>

def test_<function>_returns_expected_value():
    assert <function>(valid_input) == expected_output

def test_<function>_raises_on_invalid_input():
    with pytest.raises(ValueError):
        <function>(invalid_input)
```

### TypeScript test conventions

```typescript
import { describe, it, expect, beforeEach } from 'vitest';
import { methodName } from '../src/<module>';

describe('<module>', () => {
  beforeEach(() => { /* reset state */ });

  it('methodName does expected thing with valid input', () => {
    expect(methodName(validInput)).toBe(expectedOutput);
  });

  it('methodName throws on invalid input', () => {
    expect(() => methodName(invalidInput)).toThrow();
  });
});
```

---

## Step 4 — Verify CI runs tests

Check `.github/workflows/` for a workflow that actually **runs** tests for the service (not just a coverage estimation script).

If no test-execution workflow exists, create or update `.github/workflows/test.yml` with a job for the service. Use the appropriate setup action and test command for the language:

**Java:**
```yaml
- uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'
- run: mvn test
  working-directory: <service-directory>
```

**Python:**
```yaml
- uses: actions/setup-python@v5
  with:
    python-version: '3.11'
- run: pip install pytest
- run: pytest
  working-directory: <service-directory>
```

**TypeScript:**
```yaml
- uses: actions/setup-node@v4
  with:
    node-version: '20'
- run: npm install
  working-directory: <service-directory>
- run: npm test
  working-directory: <service-directory>
```

All jobs should trigger on `pull_request` and `push` to `main`.

---

## Step 5 — Generate coverage audit report

After all tests are written and the CI workflow passes:

1. Run `python3 coverage_report.py` from the repo root to get updated coverage numbers.
2. Create a new file in `audit_reports/` named `coverage-audit-PR-<PR_NUMBER>.md`.
3. The report must follow the standard audit report format (see the devin-trigger prompt for the template).
4. Include a **Compliance Findings** section listing any gaps or implementation issues discovered during Step 2.
5. Commit the report to the PR branch.

---

## Checklist

Before marking the task complete, verify every item:

- [ ] Test framework configured (`pom.xml` / `package.json` / CI environment)
- [ ] Test directory exists with correct structure for the language
- [ ] Test file exists for every source file touched
- [ ] Every compliance-critical function identified in Step 2 has tests
- [ ] All case categories covered: happy path, null/malformed input, boundary values, failure paths
- [ ] No raw PII in assertions, log output, or test names
- [ ] `.github/workflows/test.yml` exists and all jobs pass
- [ ] `audit_reports/coverage-audit-PR-<N>.md` created and committed
