---
description: Rules for handling PII in tests across ClearBank Core. Reference this whenever writing or reviewing tests that touch user data, account numbers, SSNs, emails, or any other personally identifiable information.
---

# PII Handling in Tests

ClearBank Core processes sensitive personal data (SSNs, account numbers, email addresses, names). These rules apply to every test across all services — Java, Python, and TypeScript.

## Core Rule

**All PII fields must be masked in test assertions — never assert on, log, or print raw PII values.**

## What Counts as PII

- Social Security Numbers (SSNs) — e.g. `123-45-6789`
- Account numbers — e.g. `ACC001`, full account identifiers
- Email addresses
- Full names or partial names tied to an individual
- Any field passed through `redact_record` / `mask_ssn` / `mask_account` in the pii-service

## Assertions

- Assert on the **masked or redacted form**, not the original value.
  - SSN: assert the result equals `***-**-6789` (last-four only), not `123-45-6789`.
  - Account number: assert the result equals `****01` (last-two only), not `ACC001`.
  - Email: assert the result equals `t***@example.com` (masked local part), not `test@example.com`.
- When testing that a field is present, assert on its **shape** (e.g. matches `***-**-\d{4}`) rather than its literal value.
- When testing error paths (e.g. `ValueError` on a malformed SSN), use obviously fake, structurally-invalid strings (e.g. `"not-an-ssn"`, `"000-00-0000"`) — not real-looking PII.

## Logging & Output

- **Never** use `print`, `System.out.println`, `console.log`, `logging.*`, or any other output mechanism to emit a raw PII value inside a test — including in debug statements, failure messages, or `@BeforeEach` setup.
- If a test failure message needs to reference a field for debugging, reference its **masked form** or a **placeholder** (e.g. `"SSN field"`, `"account ACC***"`).
- Do not capture raw PII in `ArgumentCaptor` assertions where the captured value would be printed by the test runner on failure — use a regex or structural check instead.

## Test Data

- Use **synthetic, obviously fake** PII in fixtures and test data:
  - SSNs: `123-45-6789`, `987-65-4321` (clearly fake patterns are fine as input to the redactor; they must not appear in assertions).
  - Account numbers: `ACC001`, `ACC999` (the seeded demo accounts).
  - Emails: `user@example.com`, `test@test.invalid`.
- Do not use real customer data, production samples, or data derived from real individuals in any test fixture.

## General Rules

1. **Mask before assert.** Always call the relevant masking function first, then assert on its output.
2. **No raw PII in test names or descriptions.** `@Test void loginWithSSN123456789()` is forbidden; use `@Test void loginWithValidSSN()` instead.
3. **These rules apply everywhere** — unit tests, integration tests, parameterised tests, and test helpers / utilities.
