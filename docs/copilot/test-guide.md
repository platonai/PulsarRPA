# Browser4 Testing Guide (v2025-10-14)

This guide centralizes all test-related practices for Browser4. The main AI Copilot guide remains minimal and links here for details.

- Repository type: Multi-module Maven (use Maven wrapper)
- Primary language: Kotlin with Java interop
- OS note (Windows cmd.exe): Use Windows-friendly quotes for properties and test globs
  - Examples: `-D"spotless.apply.skip=true"`, `-Dtest="*E2ETest*"`

---

## 1. Principles & Scope

- Include focused unit tests for new/changed logic (happy path + at least one edge case)
- Testing integration: Prefer unit tests by default; add integration/E2E where behavior spans modules or browser automation
- External/LLM code: Inject strategy/provider to enable deterministic offline tests (no live keys required)
- Security: Never log secrets in tests; sanitize fixtures
- Performance: Tests should run quickly; tag heavy/long tests accordingly

Definition of Done (test-related):
1. Build passes locally
2. For logic changes, tests cover main path + one boundary case
3. Coverage meets targets (see below) or deviations are justified

---

## 2. Strategy & Coverage Targets

Mix guidelines:
- Unit ≈ 70%
- Integration ≈ 25%
- E2E ≈ 5%

Coverage targets:
- Global ≥ 70%
- Core logic ≥ 80%
- Utilities ≥ 90%
- Controllers ≥ 85%

Zero tolerance for flaky tests.

---

## 3. Layout & Naming

- Module unit tests under: `src/test/kotlin/...`
- Centralized integration/E2E: `pulsar-tests/` (shared utilities: `pulsar-tests-common/`)

Naming conventions:
- Unit: `<ClassName>Test.kt`
- Integration: `<ClassName>IT.kt` or `<ClassName>Test.kt`
- E2E: `<ClassName>E2ETest.kt`
- Method names: Backticks + BDD style, e.g. ``Given X When Y Then Z``

---

## 4. Tags (JUnit 5 @Tag)

- `UnitTest`, `IntegrationTest`, `E2ETest`, `ExternalServiceTest`, `TimeConsumingTest`, `HeavyTest`, `SmokeTest`, `BenchmarkTest`

Usage examples in `docs/copilot/templates/test-tag-usage.md`.

---

## 5. Base Architecture & Utilities

- `pulsar-tests/`: Centralized integration/E2E
- `pulsar-tests-common/`: Shared base classes and utilities
- Base classes:
  - `TestBase`: Core test configuration & Spring context
  - `TestWebSiteAccess`: Auto-starts local mock server for page access
  - `WebDriverTestBase`: Browser automation tests
- Test website resources: `pulsar-tests/src/main/resources/static/`
  - Generated: `pulsar-tests-common/src/main/resources/static/generated/`

---

## 6. Configuration & Environment

- Spring relaxed binding applies: `first-name ≈ first_name ≈ firstName ≈ first.name`; `server.port ≈ SERVER_PORT`
- Profiles: `test`, `integration`, `e2e`
- Place test data under `src/test/resources/` (configs, fixtures, datasets, static assets)

---

## 7. Mocking & Test Doubles

- Unit tests: mock external dependencies aggressively
- Integration tests: use real Spring beans, mock only true externals
- E2E: minimize mocking; prefer real systems and browser automation

---

## 8. CI/CD & Parallelism

- Execution phases: Unit (per commit) → Integration (PR/main) → E2E (nightly/pre-release) → Performance (weekly/pre-major)
- Surefire parallel example:
  ```xml
  <parallel>methods</parallel>
  <threadCount>4</threadCount>
  <perCoreThreadCount>true</perCoreThreadCount>
  ```
- Reporting: JUnit XML; JaCoCo; preserve failed artifacts

---

## 9. Commands

Cross-platform examples using Maven wrapper:

- Build without tests:
  - Unix/macOS: `./mvnw -q -DskipTests`
  - Windows: `mvnw.cmd -q -DskipTests`

- Selective tests:
  - Unix/macOS: `./mvnw test -Dtest="*CoreTest"`
  - Windows: `mvnw.cmd test -Dtest="*CoreTest"`

- Module tests:
  - Unix/macOS: `./mvnw -pl pulsar-core -am test`
  - Windows: `mvnw.cmd -pl pulsar-core -am test`

- E2E examples (Windows):
  - `mvnw.cmd -pl pulsar-tests -Dtest="*ChromeDomServiceE2ETest*" test -D"spotless.apply.skip=true"`
  - `mvnw.cmd -pl pulsar-tests -Dtest="*E2ETest*" test`

---

## 10. Tools & Assertions

- Prefer AssertJ or Kotlin test DSL
- Browser automation: Selenium/WebDriver (Chrome via CDP as configured)
- Spring Boot Test, MockK, TestContainers where applicable

---

## 11. Benchmarks

For microbenchmarks, use the dedicated JMH module `pulsar-benchmarks`.

Build and run:
```
./mvnw -pl pulsar-benchmarks -am package -DskipTests
java -jar pulsar-benchmarks/target/pulsar-benchmarks-*-shaded.jar -f 1 -wi 3 -i 5
```
Guidelines:
- Name: `<Domain><Operation>Benchmark`
- Avoid I/O; prepare data in `@Setup`
- Brief comment: goal + metrics + regression triggers
- No logging/I/O inside the timed loop

---

## 12. Templates & Resources

- `docs/copilot/templates/test-tag-usage.md` – Tag examples
- PR hygiene for tests and flakes: include failure signature, recent runtime, tag; use `docs/copilot/templates/pr-description-template.md`

---

## 13. Quick Reference (Testing)

- Test naming: `XxxTest` / `XxxIT` / `XxxE2ETest`
- Test tags: `UnitTest` / `IntegrationTest` / `E2ETest` / ...
- Coverage: Global ≥ 70% / Core ≥ 80% / Utilities ≥ 90% / Controllers ≥ 85%
- Commands: see Section 9

---

## 14. Future Improvements (Testing)

- Aggregate JaCoCo across modules and automate delta report
- More domain scenarios in tests and benchmarks
- Optional CI performance guard with thresholds
- Auto-generate test gap report (class vs test coverage skeleton)
- Flaky dashboard (frequency / first-seen)

