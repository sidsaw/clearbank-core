---
description: Which mocking and test-double libraries to use per language in ClearBank Core. Reference this when adding or modifying tests — pick the section matching the language of the code under test.
---

# Mocking & Test-Double Libraries by Language

ClearBank Core is a polyglot monorepo. Each language has a single deliberate mocking approach — do not introduce additional mocking libraries without changing this document first. Pick the section below based on the language of the code being tested.

## Java

- **Test framework:** JUnit 5.
- **Mocking:** **Mockito 5.7.x** for collaborator mocking. Hand-written stubs / fakes (e.g. a `FakeUserRepository` backed by an in-memory `Map`, a `StubClock` returning a fixed `Instant`) are preferred for simple, security-sensitive, or highly readable cases.
- **Conventions:**
  - Use `@Mock` for collaborators and `@InjectMocks` for the unit under test.
  - Use `verify(...)` to assert on side effects (ledger writes, event publishes, audit emits).
  - Use `ArgumentCaptor` when you need to assert on the shape of arguments passed to a mock.
  - When using hand-written fakes, instantiate fresh fakes in each `@BeforeEach` — never share mutable fake state across tests.
- **Forbidden:** PowerMock (and PowerMockito). If you feel you need PowerMock, the design is wrong — refactor to inject the dependency instead of static-mocking it.

## Python

- **Test framework:** `pytest`.
- **Mocking:** **`unittest.mock` from the Python standard library only.** No third-party mock libraries (no `pytest-mock`, no `mock`, no `mocker` fixtures, no `responses`, no `freezegun` — use stdlib `unittest.mock` patches instead).
- **Conventions:**
  - Use `@patch("module.path.to.symbol")` (decorator or context manager) to patch dependencies.
  - Use `MagicMock` (or `Mock`) for collaborators that need attribute / method access.
  - Patch at the *use site*, not the *definition site* (e.g. `@patch("pii_service.redactor.fetch_policy")`, not `@patch("pii_service.policies.fetch_policy")`).
  - Prefer `@patch` decorators / `with patch(...)` context managers (auto-reset). For module-level patches, stop them in `tearDown` / a fixture finalizer.
- **Forbidden:** `pytest-mock`, the third-party `mock` package, `responses`, `freezegun`, and any other third-party mocking library.

## TypeScript

- **Test framework / mocking:** **Vitest**.
- **Conventions:**
  - `vi.fn()` for standalone function mocks.
  - `vi.spyOn(obj, "method")` to wrap an existing method while preserving the original (or override with `.mockImplementation(...)`).
  - `vi.mock("module-path")` for module-level mocks; place at the top of the test file.
  - Call `vi.restoreAllMocks()` (and `vi.resetAllMocks()` where appropriate) in `afterEach`, or set `restoreMocks: true` in the Vitest config.
- **Forbidden:** `jest`, `sinon`, `ts-mockito`, `testdouble`. Do not mix mocking frameworks; Vitest covers every case we need.

## General Rules

1. **One mocking library per language.** Do not mix (e.g. Mockito + hand-rolled fakes side-by-side in the same test, or Vitest + sinon). Pick the language's designated tool from above.
2. **Prefer the simplest test double that proves the behavior.** Reach order: real object → fake → stub → spy → mock. Only use a full mock when you need to verify *interactions*; otherwise a stub or fake is clearer.
3. **Never mock the unit under test.** Mock its collaborators, not itself. If you find yourself partially mocking the class you're testing, the design needs to be split.
4. **Reset mocks between tests.** Follow the per-language reset guidance above (Mockito `@ExtendWith(MockitoExtension.class)` or `Mockito.reset`, stdlib `unittest.mock` auto-reset via decorators / context managers, Vitest `restoreAllMocks` in `afterEach`).
