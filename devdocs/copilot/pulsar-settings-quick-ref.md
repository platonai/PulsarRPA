# PulsarSettings Quick Reference Card

> Ultra-compact reference for AI agents. For detailed guide see [pulsar-settings-guide.md](./pulsar-settings-guide.md)

## Essential Pattern (Copy-Paste Ready)

```kotlin
// ALWAYS call before creating session
PulsarSettings.withDefaultBrowser()  // or other mode
val session = PulsarContexts.createSession()
```

## Browser Profile Modes

| Method | Description | Persistence |
|--------|-------------|-------------|
| `withDefaultBrowser()` | Isolated Browser4 profile | ✓ Preserved |
| `withSystemDefaultBrowser()` | System browser profile | ✓ Preserved |
| `withPrototypeBrowser()` | Base for inheritance | ✓ Preserved |
| `withSequentialBrowsers(n)` | Rotating pool of n profiles | ✓ Preserved |
| `withTemporaryBrowser()` | Fresh per instance | ✗ Discarded |

## Display Modes

```kotlin
PulsarSettings.headless()     // No window, fastest
PulsarSettings.withGUI()      // Visible window
PulsarSettings.supervised()   // Xvfb (Linux only)
```

## Common Configurations

### Desktop Development
```kotlin
PulsarSettings.withDefaultBrowser().withGUI()
```

### Headless Spider
```kotlin
PulsarSettings
    .withSequentialBrowsers(4)
    .headless()
    .maxOpenTabs(8)
    .enableUrlBlocking()
```

### Interactive RPA
```kotlin
PulsarSettings
    .withDefaultBrowser()
    .withGUI()
    .withSPA()
```

### Fast Crawling
```kotlin
PulsarSettings
    .headless()
    .enableUrlBlocking()
    .withInteractLevel(InteractLevel.FASTEST)
```

### High Quality Data
```kotlin
PulsarSettings
    .withDefaultBrowser()
    .withInteractLevel(InteractLevel.GOOD_DATA)
```

## Interaction Levels (Speed → Quality)

| Level | Scrolls | Best For |
|-------|---------|----------|
| `FASTEST` | 0 | Maximum speed |
| `FASTER` | 0 | Quick extraction |
| `FAST` | 0 | Speed-focused |
| `DEFAULT` | 1 | Balanced |
| `GOOD_DATA` | 2 | Better quality |
| `BETTER_DATA` | 3 | Higher quality |
| `BEST_DATA` | 5 | Maximum quality |

## Context Limits

```kotlin
PulsarSettings.maxBrowserContexts(4)  // Concurrent browsers (1-50)
PulsarSettings.maxOpenTabs(8)         // Tabs per browser (1-50)
```

## Resource Control

```kotlin
PulsarSettings.enableUrlBlocking()      // Block resources (100%)
PulsarSettings.enableUrlBlocking(0.8f)  // Block 80% of resources
PulsarSettings.disableUrlBlocking()     // Allow all resources
PulsarSettings.blockImages()            // Block images specifically
```

## LLM Configuration

```kotlin
PulsarSettings.withLLM("volcengine", "model-name", apiKey)
// Or step by step:
PulsarSettings
    .withLLMProvider("volcengine")
    .withLLMName("model-name")
    .withLLMAPIKey(apiKey)
```

## Method Chaining Examples

```kotlin
// All methods return PulsarSettings.Companion for chaining
PulsarSettings
    .withDefaultBrowser()
    .withGUI()
    .maxBrowserContexts(2)
    .maxOpenTabs(8)
    .withInteractLevel(InteractLevel.GOOD_DATA)
    .enableUrlBlocking()
```

## Data Class Usage

```kotlin
val settings = PulsarSettings(
    spa = true,
    displayMode = DisplayMode.GUI,
    maxBrowsers = 2,
    maxOpenTabs = 8,
    profileMode = BrowserProfileMode.DEFAULT
)
settings.overrideSystemProperties()
```

## Decision Quick Guide

```
Need GUI? → .withGUI()
Need speed? → .headless() + .enableUrlBlocking() + InteractLevel.FAST
Need quality data? → InteractLevel.GOOD_DATA or BEST_DATA
Need isolation? → .withTemporaryBrowser()
Need parallel browsers? → .withSequentialBrowsers(n)
Need SPA interaction? → .withSPA()
Need AI features? → .withLLM(provider, name, key)
```

## Critical Rule

⚠️ **ALWAYS** call PulsarSettings **BEFORE** creating session:

```kotlin
// ✓ CORRECT
PulsarSettings.headless()
val session = PulsarContexts.createSession()

// ✗ WRONG - settings ignored
val session = PulsarContexts.createSession()
PulsarSettings.headless()  // Too late!
```

---

**Quick Links**:
- [Full Guide](./pulsar-settings-guide.md)
- [LoadOptions Guide](./load-options-guide.md)
- [Config Guide](../../docs/config.md)

**Version**: 2026-01-10
