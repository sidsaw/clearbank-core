---
description: Which mocking and test-double libraries to use in each ClearBank Core service. Reference this when adding or modifying tests to ensure the correct mocking approach per language/service.
---

# Mocking & Test-Double Libraries by Service

ClearBank Core is a polyglot monorepo. Each service has a deliberate, single mocking approach — do not introduce additional mocking libraries without changing this document first.

## `auth-service` (Java)

- **Test framework:** JUnit 5 only.
- **Mocking:** **No Mockito.** Use **hand-written stubs / fakes** for collaborators (e.g. a `FakeUserRepository` that stores users in an in-memory `Map`, a `StubClock` that returns a fixed `Instant`).
- **Reference:** see existing `auth-service/src/test/AuthTest.java` for the established stub-based style — copy its conventions when adding new tests.
- **Why:** auth flows are simple and the team prefers explicit, readable test doubles over mock-DSL incantations for security-sensitive logic.

## `transaction-service` (Java)

- **Test framework:** JUnit 5.
- **Mocking:** **Mockito 5.7.x** (already declared in `transaction-service/pom.xml` — do not bump or add alternatives).
- **Conventions:**
  - Use `@Mock` for collaborators and `@InjectMocks` for the unit under test.
  - Use `verify(...)` to assert on side effects (ledger writes, event publishes, audit emits).
  - Use `ArgumentCaptor` when you need to assert on the shape of arguments passed to a mock.
- **Forbidden:** PowerMock (and PowerMockito). If you feel you need PowerMock, the design is wrong — refactor to inject the dependency instead of static-mocking it.

## `pii-service` (Python)

- **Test framework:** `pytest`.
- **Mocking:** **`unittest.mock` from the Python standard library only.** No third-party mock libraries (no `pytest-mock`, no `mock`, no `mocker` fixtures, no `responses`, no `freezegun` — use stdlib `unittest.mock` patches instead).
- **Conventions:**
  - Use `@patch("module.path.to.symbol")` (decorator or context manager) to patch dependencies.
  - Use `MagicMock` (or `Mock`) for collaborators that need attribute / method access.
  - Patch at the *use site*, not the *definition site* (e.g. `@patch("pii_service.redactor.fetch_policy")`, not `@patch("pii_service.policies.fetch_policy")`).

## `audit-service` (TypeScript)

- **Test framework / mocking:** **Vitest** — add it to `audit-service/package.json` `devDependencies` if not already present.
- **Conventions:**
  - `vi.fn()` for standalone function mocks.
  - `vi.spyOn(obj, "method")` to wrap an existing method while preserving the original (or override with `.mockImplementation(...)`).
  - `vi.mock("module-path")` for module-level mocks; place at the top of the test file.
- **Forbidden:** `jest`, `sinon`, `ts-mockito`, `testdouble`. Do not mix mocking frameworks; Vitest covers every case we need.

## General Rules

1. **One mocking library per service.** Do not mix (e.g. Mockito + hand-rolled fakes side-by-side, or Vitest + sinon). Pick the service's designated tool from above.
2. **Prefer the simplest test double that proves the behavior.** Reach order: real object → fake → stub → spy → mock. Only use a full mock when you need to verify *interactions*; otherwise a stub or fake is clearer.
3. **Never mock the unit under test.** Mock its collaborators, not itself. If you find yourself partially mocking the class you're testing, the design needs to be split.
4. **Reset mocks between tests.** Per-language guidance:
   - Mockito: rely on `MockitoExtension` (`@ExtendWith(MockitoExtension.class)`) which resets per test, or call `Mockito.reset(...)` in `@AfterEach` if you manage mocks manually.
   - `unittest.mock`: prefer `@patch` decorators / `with patch(...)` context managers (auto-reset). For module-level patches, stop them in `tearDown` / a fixture finalizer.
   - Vitest: call `vi.restoreAllMocks()` (and `vi.resetAllMocks()` where appropriate) in `afterEach`, or set `restoreMocks: true` in the Vitest config.
   - Hand-written fakes (auth-service): instantiate fresh fakes in each `@BeforeEach` — never share mutable fake state across tests.
