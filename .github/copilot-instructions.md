# Browser4 Copilot Instructions
- Multi-module Maven project primarily in Kotlin (see `pom.xml`); keep edits compatible with Java code that still exists.
- Default entrypoint is `ai.platon.pulsar.app.PulsarApplicationKt` in `pulsar-rest`, exposing REST + command endpoints on :8182 with LLM-backed automation.
- Runtime depends on LLM API keys (e.g., `DEEPSEEK_API_KEY`, `OPENAI_API_KEY`); never hardcode them—use env vars as in README examples.
## Architecture
- `pulsar-core/` hosts the engine: crawler scheduler, DOM/session abstractions; start at `pulsar-skeleton/.../PulsarSession.kt` and `browser/driver` for browser control.
- `pulsar-rest/` packages Spring Boot services and command ingestion (see `rest/api/service/ScrapeService.kt`) bridging HTTP and pulsar sessions.
- `pulsar-client/` supplies reusable client SDK + CLI utilities for embedding Browser4 in other JVM apps.
- `browser4/` aggregates product-facing modules: `browser4-spa` (web UI) and `browser4-crawler` (deployment packaging, Docker entrypoints).
- Tests live in `pulsar-tests/` and `pulsar-tests-common/`; heavy scenarios tagged via JUnit @Tag to control cost.
## Key Concepts
- Sessions are created through `PulsarContexts.createSession()` for basic tasks and `AgenticContexts.createSession()` for agentic tasks; tasks flow from URL pools -> crawl loop -> session load/parse -> extraction; review `docs/concepts.md` for vocabulary.
- Load options are expressed as CLI-like flags inside URL strings; reuse `LoadOptions` parsing utilities instead of manual string handling.
- Browser automation uses `ai.platon.pulsar.browser` packages; examine `PageHandler` and `ClickableDOM` for interaction patterns before changing selectors.
## Build & Test
- Preferred build scripts: Windows `bin/build.ps1 [-test]`, Linux/macOS `bin/build.sh [-test]`; they wrap multi-module Maven with proper profiles.
- Direct Maven: `./mvnw -Pall-modules test` for full suite, add `-pl <module> -am` for scoped runs; unit tests should stay under 100 ms and carry tags from `docs/copilot/templates/test-tag-usage.md`.
- Coverage enforced via Jacoco in the `ci` profile (70% instruction minimum); keep new code covered or justify deviations.
## Coding Patterns
- Follow Kotlin-first style from `docs/copilot/README-AI.md`: immutable data classes, explicit return types, and logger placeholders (no string interpolation in log messages).
- Public APIs require KDoc summarizing intent, parameters, return, and exceptions; update adjacent docs when behavior shifts (see `docs/advanced-guides.md`).
- Configuration overrides belong in layered `application*.properties`; avoid scattering hardcoded defaults in code.
## Debug & Tooling
- Use `pulsar-tests` heavy suites for diagnosing browser issues; many tests expect Chrome via CDP—ensure devtools binary path comes from existing configuration beans.
- Logs route through structured format described in `docs/log-format.md`; preserve JSON-compatible message layout when adding fields.
- For performance regressions, run `./mvnw -pl pulsar-benchmarks -am package -DskipTests`, then execute the shaded JMH jar as documented.
- Windows-friendly quoting for -D properties, e.g. `-D"spotless.apply.skip=true"`
## Integration Tips
- REST features expose text-based "commands" and X-SQL endpoints; refer to `docs/rest-api-examples.md` and reuse helper DTOs in `pulsar-rest/.../dto` instead of inventing new payloads.
- When extending extraction logic, prefer registering `ParseFilter` or `PageEventHandlers` in `pulsar-core` rather than branching inside service layers.
- SPA support lives in `browser4-spa/src`; React/TypeScript bundles are built via root `package.json` scripts and served by the Spring Boot app.
## Common Pitfalls
- Many modules share shaded dependencies through the parent BOM; never bump versions locally—edit the parent if absolutely necessary.
- The Chrome DevTools RPC layer throws `ChromeRPCException`; wrap new calls with existing retry utilities to avoid flooding logs (see `pulsar-core/pulsar-tools/pulsar-browser`).
- Long-running crawls rely on privacy context rotation; changes touching proxy/rotation code must keep event handlers idempotent and thread-safe.
