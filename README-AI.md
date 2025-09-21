# 🚦 AI Coder Agent Guideline

This guideline defines how AI Coder Agents (e.g., GitHub Copilot, Claude Coder, ChatGPT) should behave when contributing 
to this project. The document standardizes **environment setup**, **coding rules**, **testing strategy**, 
and **CI/CD integration**.

> 💡 **Tip:** This file should be upgrade frequently

---

## 1. 🛠 Environment Setup

- **Project Type**: Multi-module **Maven** project
- **Primary Language**: **Kotlin**
- **Build Tool**: Always use `./mvnw` (Maven wrapper) from the project root
- **System Adaptation**: Detect **OS environment first** to select best-suited tools  
- **Java Compatibility**: Read `pom.xml` detemine Java versions

---

## 2. 📖 General Coding Guidelines

- Prefer **data classes** for DTOs and state holders
- Keep functions **small and single-responsibility**
- Document all **public functions and classes** with KDoc

---

## 3. 🤖 AI Agent Behavior

---

## 4. 📂 Project Structure Rules

- **Root layout**:
```
project-root/
├── pulsar-core/            # Core modules
│    ├── pulsar-skeleton/   # Core WebDriver & AI translation logic
│    ├── pulsar-common/     # Common utilities and shared code
│    ├── pulsar-dom/        # DOM manipulation and parsing
│    ├── pulsar-persist/    # Data persistence layer
│    ├── pulsar-plugins/    # Plugin system
│    ├── pulsar-ql/         # Query language implementation
│    ├── pulsar-ql-common/  # Query language common components
│    ├── pulsar-resources/  # Resource management
│    ├── pulsar-spring-support/ # Spring framework integration
│    ├── pulsar-third/      # Third-party integrations
│    └── pulsar-tools/      # Development and utility tools
├── pulsar-rest/            # REST API and web services
├── pulsar-client/          # Client libraries and SDKs
├── pulsar-tests/           # Centralized test packages
├── pulsar-examples/        # Example code and tutorials
├── browser4/              # Browser automation modules
│    ├── browser4-crawler/ # Web crawling functionality
│    └── browser4-spa/     # Single Page Application support
├── pulsar-all/            # Aggregation module
├── pulsar-bom/            # Bill of Materials
└── pom.xml                # Root Maven configuration
```

- **Module guidelines**:
  - Each module must define its own `pom.xml`
  - No cyclic dependencies between modules
  - Shared utilities go to `pulsar-core/pulsar-common`
  - AI client/utilities in `pulsar-third/pulsar-llm`

---

## 5. 🗂 File Naming & Package Rules

- **File Naming**:
- Kotlin files: `PascalCase.kt` (e.g., `TextToAction.kt`)
- Test files: `<ClassName>Test.kt` (e.g., `TextToActionTest.kt`)
- Integration tests: `<ClassName>IT.kt`
- Guidelines for AI: `README-AI.md` (in project root dir and multiple modules)

- **Packages**:
- Base package: `ai.platon.pulsar`
- Organized by **responsibility**, not by layer only:
  - `ai.platon.pulsar.skeleton.*` (Core WebDriver & AI translation)
  - `ai.platon.pulsar.common.*` (Common utilities)
  - `ai.platon.pulsar.dom.*` (DOM manipulation)
  - `ai.platon.pulsar.persist.*` (Data persistence)
  - `ai.platon.pulsar.ql.*` (Query language)
  - `ai.platon.pulsar.rest.*` (REST API)
  - `ai.platon.pulsar.app.*` (Application layer)
  - `ai.platon.pulsar.test.*` (Test packages)

---

## 6. 🏷 Class Placement by Responsibility

- **REST APIs & Controllers** → `pulsar-rest/src/main/kotlin/ai/platon/pulsar/rest/api/controller/`
- **API Services** → `pulsar-rest/src/main/kotlin/ai/platon/pulsar/rest/api/service/`
- **API Entities/DTOs** → `pulsar-rest/src/main/kotlin/ai/platon/pulsar/rest/api/entities/`
- **Core Session Management** → `pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/session/`
- **Web Crawling Logic** → `pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/crawl/`
- **AI Translation (Text-to-Action)** → `pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/ai/`
- **DOM Manipulation & Models** → `pulsar-dom/src/main/kotlin/ai/platon/pulsar/dom/`
- **Common Utilities** → `pulsar-common/src/main/kotlin/ai/platon/pulsar/common/`
- **Data Persistence** → `pulsar-persist/src/main/kotlin/ai/platon/pulsar/persist/`
- **Query Language** → `pulsar-ql/src/main/kotlin/ai/platon/pulsar/ql/`
- **Browser Automation Apps** → `browser4/browser4-crawler/src/main/kotlin/ai/platon/pulsar/app/`
- **Test Classes** → Mirror the main package structure under `src/test/kotlin/`

---

## 7. 🧪 Testing Rules

- **Test Module**: Integration tests and E2E tests are centralized in `pulsar-tests/` module
  - Test website: Inherit from `TestWebSiteAccess` to start the test website
  - Test webpages: Located in `pulsar-tests/src/main/resources/static`
  - Test webdriver: Inherit from `WebDriverTestBase` to create webdrivers for testing
- **Test Naming Rules**:
  - Test files: `<ClassName>Tests.kt` or `<ClassName>Test.kt`
  - Method names: Use backticks for descriptive test names (e.g., `` `When ask to click a button then generate correct WebDriver action code`() ``)
  - Integration tests can use `<ClassName>IT.kt` pattern
- **Test Annotations**:
  - Use `@Tag("ExternalServiceTest")` for tests requiring external services
  - Use `@Tag("TimeConsumingTest")` for long-running tests
- **Coverage Expectations**:
  - **JaCoCo** configured for CI profile with minimum 70% instruction coverage
  - **Unit tests**: Focus on core logic and utilities
  - **Integration tests**: Critical paths and REST API endpoints
  - **E2E tests**: AI → WebDriver command correctness and browser automation

---

## 8. ⚙️ CI/CD Integration

- **Pipeline**: GitHub Actions
- **Workflow**:
1. Detect version from `VERSION` file
2. Build with `./mvnw clean install`
3. Run unit + integration tests
4. Build Docker image → Run integration/E2E validation
5. If all pass:
   - Deploy artifacts to Sonatype
   - Push Docker images to registry
- **Quality Gates**:
- Lint check (`ktlint`, `detekt`) must pass
- Tests must succeed (no flaky tests allowed)
- Minimum coverage enforced in CI

---
