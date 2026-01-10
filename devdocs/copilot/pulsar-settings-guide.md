# PulsarSettings AI-Friendly Guide

## Overview

`PulsarSettings` is the **global configuration class** for controlling Browser4's browser behavior, display mode, and interaction settings. It provides a fluent API for setting up the browser environment before creating sessions.

**Location**: `ai.platon.pulsar.skeleton.PulsarSettings`

**Key Characteristics**:
- Fluent API with method chaining (builder pattern)
- Must be called **before** creating `PulsarSession` or `AgenticSession`
- Settings apply globally via JVM system properties
- Supports both Kotlin and Java with `@JvmStatic` annotations
- Can optionally accept a `MutableConfig` for session-specific overrides

---

## Quick Reference

### Most Common Patterns

```kotlin
// Desktop usage: Use default isolated browser profile
PulsarSettings.withDefaultBrowser()
val session = PulsarContexts.createSession()

// Spider mode: Headless with multiple browsers
PulsarSettings
    .headless()
    .maxBrowserContexts(4)
    .maxOpenTabs(12)
    .enableUrlBlocking()

// Interactive SPA mode: GUI with your default browser profile
PulsarSettings.withSystemDefaultBrowser().withGUI().withSPA()

// High-performance crawling with sequential browsers
PulsarSettings
    .withSequentialBrowsers(BrowserType.DEFAULT, 10)
    .headless()
    .enableUrlBlocking()
```

---

## Browser Profile Modes

PulsarSettings controls how browser profiles (user data directories) are managed.

### Mode Comparison Table

| Mode | Method | Description | Persistence | Use Case |
|------|--------|-------------|-------------|----------|
| `DEFAULT` | `withDefaultBrowser()` | Isolated Browser4-managed profile | Preserved | General purpose |
| `SYSTEM_DEFAULT` | `withSystemDefaultBrowser()` | Your system browser's profile | Preserved | Quick testing, debugging |
| `PROTOTYPE` | `withPrototypeBrowser()` | Base profile for inheritance | Preserved | State inheritance |
| `SEQUENTIAL` | `withSequentialBrowsers()` | Pool of rotating profiles | Preserved | Batch crawling |
| `TEMPORARY` | `withTemporaryBrowser()` | New profile per browser | Discarded | Stateless crawling |

### Detailed Mode Descriptions

#### DEFAULT (Recommended for Desktop)

```kotlin
// Uses Browser4's isolated profile, preserves cookies and history
PulsarSettings.withDefaultBrowser()
```

- Creates an isolated user data directory managed by Browser4
- Any modifications (cookies, history, etc.) are preserved
- Single browser context by default
- Best for: General purpose, desktop usage, development

#### SYSTEM_DEFAULT

```kotlin
// Uses your daily-used browser profile
PulsarSettings.withSystemDefaultBrowser()
```

- Uses your system's default Chrome/Edge profile
- Shares your real session data, cookies, login states
- **WARNING**: No longer supported by Chrome since v143 (see [Issue #162](https://github.com/platonai/Browser4/issues/162))
- Best for: Quick debugging with real session data

#### PROTOTYPE

```kotlin
// Predefined base profile for SEQUENTIAL and TEMPORARY modes
PulsarSettings.withPrototypeBrowser()
```

- Acts as the base profile that other modes inherit from
- Single browser context
- Best for: Preparing a base state (login, cookies) for batch operations

#### SEQUENTIAL (Advanced)

```kotlin
// Pool of 10 rotating profiles
PulsarSettings.withSequentialBrowsers(BrowserType.DEFAULT, 10)
```

- Selects profiles from a managed pool sequentially
- Profiles inherit from PROTOTYPE profile
- Supports multiple concurrent browser contexts
- Best for: High-performance batch crawling, avoiding session reuse

#### TEMPORARY (Advanced)

```kotlin
// Fresh profile for each browser instance
PulsarSettings.withTemporaryBrowser()
```

- Creates new isolated profile for each browser instance
- Profile is discarded after session ends
- Best for: Maximum isolation, stateless crawling

---

## Display Modes

Control how browsers are launched.

### Available Methods

| Method | Display Mode | Description |
|--------|--------------|-------------|
| `headless()` | HEADLESS | No visible window, fastest |
| `headed()` / `withGUI()` | GUI | Visible browser window |
| `supervised()` | SUPERVISED | Uses Xvfb (Linux only) |

### Examples

```kotlin
// Headless for server/CI environments
PulsarSettings.headless()

// GUI for development and debugging
PulsarSettings.withGUI()

// Supervised mode for Linux servers needing GUI simulation
PulsarSettings.supervised()
```

### Environment Detection

```kotlin
// BrowserSettings automatically detects environment:
// - Falls back to headless if no display available
// - Logs warning: "The current environment has no GUI support, fallback to headless mode"
```

---

## Browser Context Configuration

### Maximum Browser Contexts

```kotlin
// Set maximum concurrent browser contexts (isolated browser instances)
PulsarSettings.maxBrowserContexts(4)
```

- Each context has its own cookies, local storage, and cache
- Default varies by profile mode (1 for DEFAULT/SYSTEM_DEFAULT/PROTOTYPE)
- Maximum recommended: 50 (higher values may cause disk space issues)

### Maximum Open Tabs

```kotlin
// Set maximum tabs per browser context
PulsarSettings.maxOpenTabs(12)
```

- Controls concurrency within each browser context
- Range: 1-50
- DEFAULT/SYSTEM_DEFAULT/PROTOTYPE modes: unlimited by default

---

## SPA Mode (Single Page Application)

```kotlin
// Enable SPA mode for interactive applications
PulsarSettings.withSPA()
```

**What SPA mode does**:
- Removes timeout limits on page loads and fetches
- Sets `FETCH_TASK_TIMEOUT` to 1000 days
- Sets `PRIVACY_CONTEXT_IDLE_TIMEOUT` to 1000 days
- Enables `BROWSER_SPA_MODE` flag

**Use Case**: When you need to interact with pages indefinitely (manual interaction, RPA tasks)

---

## Interaction Settings

Control how Browser4 interacts with webpages (scrolling, clicking, waiting).

### Using Interaction Levels

```kotlin
// Use preset interaction levels
PulsarSettings.withInteractLevel(InteractLevel.FASTEST)  // Maximum speed, minimal interaction
PulsarSettings.withInteractLevel(InteractLevel.FASTER)   // Very fast, slight interaction
PulsarSettings.withInteractLevel(InteractLevel.FAST)     // Fast, moderate interaction
PulsarSettings.withInteractLevel(InteractLevel.DEFAULT)  // Balanced
PulsarSettings.withInteractLevel(InteractLevel.GOOD_DATA)    // Better data, slower
PulsarSettings.withInteractLevel(InteractLevel.BETTER_DATA)  // Even better data
PulsarSettings.withInteractLevel(InteractLevel.BEST_DATA)    // Best data, slowest
```

### Interaction Level Details

| Level | Auto Scroll | Scroll Interval | Script Timeout | Page Load Timeout | Best For |
|-------|-------------|-----------------|----------------|-------------------|----------|
| FASTEST | 0 | 500ms | 30s | 2min | Fast, shallow scraping |
| FASTER | 0 | 500ms | 30s | 2min | Quick data extraction |
| FAST | 0 | 500ms | 30s | 2min | Speed-focused crawling |
| DEFAULT | 1 | 500ms | 1min | 3min | General purpose |
| GOOD_DATA | 2 | 1s | 30s | 3min | Better content extraction |
| BETTER_DATA | 3 | 1s | 30s | 3min | Higher quality data |
| BEST_DATA | 5 | 1s | 30s | 3min | Maximum data quality |

### Custom Interaction Settings

```kotlin
// Create custom interaction settings
val customSettings = InteractSettings(
    autoScrollCount = 3,
    scrollInterval = Duration.ofSeconds(1),
    scriptTimeout = Duration.ofSeconds(45),
    pageLoadTimeout = Duration.ofMinutes(2),
    bringToFront = true
)
PulsarSettings.withInteractSettings(customSettings)
```

### DOM Settle Policy

```kotlin
// Control how Browser4 determines page load completion
PulsarSettings.withDOMSettlePolicy(DomSettlePolicy.NETWORK_IDLE)
```

Available policies:
- `READY_STATE_INTERACTIVE` - DOM is interactive
- `READY_STATE_COMPLETE` - DOM fully loaded
- `NETWORK_IDLE` - No network activity
- `FIELDS_SETTLE` - Key fields stable (default)
- `HASH` - Content hash stable

---

## URL Blocking

Control browser resource loading for performance optimization.

```kotlin
// Enable URL blocking (100% probability)
PulsarSettings.enableUrlBlocking()

// Enable with custom probability (0.0-1.0)
PulsarSettings.enableUrlBlocking(0.8f)

// Disable URL blocking
PulsarSettings.disableUrlBlocking()

// Block images specifically
PulsarSettings.blockImages()
```

**Use Case**: Blocking unnecessary resources (images, scripts) to speed up crawling

---

## User Agent Configuration

```kotlin
// Enable user agent overriding (not recommended)
PulsarSettings.enableUserAgentOverriding()

// Disable user agent overriding (default)
PulsarSettings.disableUserAgentOverriding()
```

**Warning**: User agent overriding can be detected by websites and may lead to blocks.

---

## Page Export Configuration

```kotlin
// Enable automatic export of fetched pages
PulsarSettings.enableOriginalPageContentAutoExporting()

// Enable with limit
PulsarSettings.enableOriginalPageContentAutoExporting(1000)

// Disable auto-export
PulsarSettings.disableOriginalPageContentAutoExporting()
```

**Export Location**: `AppPaths.WEB_CACHE_DIR/default/pulsar_chrome/OK/{domain}`

---

## LLM Configuration

Configure Language Model providers for AI-powered features.

### Using withLLM (All-in-One)

```kotlin
// Configure all LLM settings at once
PulsarSettings.withLLM("volcengine", "ep-20250218132011-2scs8", apiKey)
```

### Individual LLM Settings

```kotlin
// Configure step by step
PulsarSettings
    .withLLMProvider("volcengine")
    .withLLMName("ep-20250218132011-2scs8")
    .withLLMAPIKey(apiKey)
```

### Supported LLM Providers

| Provider | API Documentation |
|----------|-------------------|
| Volcengine | [Volcengine API](https://www.volcengine.com/docs/82379/1399008) |
| DeepSeek | [DeepSeek API](https://api-docs.deepseek.com/) |

---

## Complete Configuration Examples

### Example 1: Desktop Development

```kotlin
// Simple desktop usage with GUI
PulsarSettings.withDefaultBrowser().withGUI()
val session = PulsarContexts.createSession()
val page = session.load(url, "-expires 1d")
```

### Example 2: High-Performance Spider

```kotlin
// Optimized for crawling many pages
PulsarSettings
    .withSequentialBrowsers(BrowserType.DEFAULT, 4)
    .headless()
    .maxOpenTabs(8)
    .enableUrlBlocking()
    .withInteractLevel(InteractLevel.FAST)

val session = PulsarContexts.createSession()
```

### Example 3: Interactive RPA

```kotlin
// For robot process automation with GUI
PulsarSettings
    .withDefaultBrowser()
    .withGUI()
    .withSPA()
    .withInteractLevel(InteractLevel.GOOD_DATA)

val session = AgenticContexts.createSession()
```

### Example 4: Temporary Isolated Sessions

```kotlin
// Maximum privacy/isolation per session
PulsarSettings
    .withTemporaryBrowser()
    .headless()

val session = PulsarContexts.createSession()
```

### Example 5: AI-Powered Crawling

```kotlin
// With LLM integration
PulsarSettings
    .withDefaultBrowser()
    .withGUI()
    .withLLM("volcengine", "ep-20250218132011-2scs8", System.getenv("LLM_API_KEY"))

val session = AgenticContexts.createSession()
```

---

## Data Class Usage

PulsarSettings can also be used as a data class for programmatic configuration:

```kotlin
// Create settings instance
val settings = PulsarSettings(
    spa = true,
    displayMode = DisplayMode.GUI,
    maxBrowsers = 2,
    maxOpenTabs = 8,
    interactSettings = InteractSettings.Builder.GOOD_DATA,
    profileMode = BrowserProfileMode.DEFAULT
)

// Apply to system properties
settings.overrideSystemProperties()

// Or apply to specific config
settings.overrideConfiguration(mutableConfig)
```

---

## Method Reference (Alphabetical)

### Browser Profile Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `withDefaultBrowser()` | `conf?` | Use Browser4-managed isolated profile |
| `withPrototypeBrowser()` | `conf?` | Use prototype profile for inheritance |
| `withSequentialBrowsers()` | `browserType?`, `maxAgents?`, `conf?` | Use rotating pool of profiles |
| `withSystemDefaultBrowser()` | `browserType?`, `conf?` | Use system browser profile |
| `withTemporaryBrowser()` | `browserType?`, `conf?` | Use fresh profile per instance |
| `withBrowser()` | `browserType`, `conf?` | Set browser type explicitly |
| `withBrowserContextMode()` | `mode`, `browserType?`, `conf?` | Set profile mode explicitly |

### Display Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `headless()` | `conf?` | Run without visible window |
| `headed()` / `withGUI()` | `conf?` | Run with visible window |
| `supervised()` | `conf?` | Run with Xvfb (Linux) |

### Context Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `maxBrowserContexts()` | `n`, `conf?` | Set max concurrent browser contexts |
| `maxOpenTabs()` | `n`, `conf?` | Set max tabs per context |
| `withSPA()` / `withSinglePageApplication()` | `conf?` | Enable SPA mode |

### Interaction Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `withInteractLevel()` | `level`, `conf?` | Use preset interaction level |
| `withInteractSettings()` | `settings`, `conf?` | Use custom interaction settings |
| `withDOMSettlePolicy()` | `policy`, `conf?` | Set DOM settle policy |

### Resource Control Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `enableUrlBlocking()` | `probability?`, `conf?` | Enable resource blocking |
| `disableUrlBlocking()` | `conf?` | Disable resource blocking |
| `blockImages()` | `conf?` | Block image resources |
| `enableUserAgentOverriding()` | `conf?` | Enable UA override |
| `disableUserAgentOverriding()` | `conf?` | Disable UA override |

### Export Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `enableOriginalPageContentAutoExporting()` | `limit?`, `conf?` | Auto-export fetched pages |
| `disableOriginalPageContentAutoExporting()` | `conf?` | Disable auto-export |

### LLM Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `withLLM()` | `provider`, `name`, `apiKey` | Configure all LLM settings |
| `withLLMProvider()` | `provider` | Set LLM provider name |
| `withLLMName()` | `name` | Set LLM model name |
| `withLLMAPIKey()` | `key` | Set LLM API key |

---

## Decision Tree

```
Setting up Browser4?
├─ Desktop development?
│   └─ PulsarSettings.withDefaultBrowser().withGUI()
├─ Server/CI environment?
│   └─ PulsarSettings.withDefaultBrowser().headless()
├─ High-performance crawling?
│   ├─ Need multiple browsers? → .withSequentialBrowsers(n)
│   ├─ Optimize resources? → .enableUrlBlocking()
│   └─ Speed priority? → .withInteractLevel(InteractLevel.FAST)
├─ Interactive/RPA mode?
│   ├─ Need unlimited time? → .withSPA()
│   └─ Need good data? → .withInteractLevel(InteractLevel.GOOD_DATA)
├─ Maximum isolation?
│   └─ PulsarSettings.withTemporaryBrowser()
└─ AI features needed?
    └─ .withLLM(provider, name, apiKey)
```

---

## Common Pitfalls & Solutions

### Problem: Settings Not Taking Effect
**Solution**: Call PulsarSettings methods **before** creating sessions

```kotlin
// ✓ Correct
PulsarSettings.withDefaultBrowser().withGUI()
val session = PulsarContexts.createSession()

// ✗ Incorrect (session already created)
val session = PulsarContexts.createSession()
PulsarSettings.withGUI() // Too late!
```

### Problem: Browser Opens in Wrong Mode
**Solution**: Check display mode is set correctly

```kotlin
// Force headless even if display available
PulsarSettings.headless()

// Or check environment
if (Runtimes.hasOnlyHeadlessBrowser()) {
    // Already in headless environment
}
```

### Problem: Too Many Browser Instances
**Solution**: Adjust maxBrowserContexts

```kotlin
// Limit to 2 concurrent browsers
PulsarSettings.maxBrowserContexts(2)
```

### Problem: Pages Loading Too Slowly
**Solution**: Use faster interaction level

```kotlin
PulsarSettings.withInteractLevel(InteractLevel.FASTEST)
// Or block unnecessary resources
PulsarSettings.enableUrlBlocking()
```

### Problem: Missing Dynamic Content
**Solution**: Use higher interaction level

```kotlin
PulsarSettings.withInteractLevel(InteractLevel.GOOD_DATA)
// Or create custom settings with more scrolling
```

### Problem: Session Data Not Persisting
**Solution**: Use correct profile mode

```kotlin
// DEFAULT preserves data
PulsarSettings.withDefaultBrowser()

// TEMPORARY discards data
PulsarSettings.withTemporaryBrowser() // Data lost after session
```

---

## Related Documentation

- [LoadOptions Guide](./load-options-guide.md) - URL-level configuration
- [Configuration Guide](../../docs/config.md) - Environment/properties configuration
- [LLM Configuration](../config/llm/llm-config.md) - LLM provider setup
- [Basic Usage Example](../../examples/browser4-examples/src/main/kotlin/ai/platon/pulsar/manual/_0_BasicUsage.kt)

---

## Source Code References

- `PulsarSettings`: `/pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/PulsarSettings.kt`
- `BrowserSettings`: `/pulsar-core/pulsar-tools/pulsar-browser/src/main/kotlin/ai/platon/pulsar/browser/common/BrowserSettings.kt`
- `InteractSettings`: `/pulsar-core/pulsar-tools/pulsar-browser/src/main/kotlin/ai/platon/pulsar/browser/common/InteractSettings.kt`
- `InteractLevel`: `/pulsar-core/pulsar-common/src/main/kotlin/ai/platon/pulsar/common/browser/InteractLevel.kt`
- `BrowserProfileMode`: `/pulsar-core/pulsar-common/src/main/kotlin/ai/platon/pulsar/common/browser/BrowserProfileMode.kt`

---

**Version**: 2026-01-10
**Maintainer**: Browser4 Team
**Status**: Living Document - Update as PulsarSettings evolves
