# PageEventHandlers - AI-Friendly Reference (v2025-01-09)

This document provides comprehensive AI-friendly documentation for the `PageEventHandlers` system, which specifies all event handlers triggered at various stages of a webpage's lifecycle.

## Overview

The `PageEventHandlers` class is the central interface for managing all events that occur during the web page lifecycle in Browser4. It organizes events into three main groups:

1. **LoadEventHandlers** - Events during the loading and parsing phase
2. **BrowseEventHandlers** - Events during the interactive browsing phase  
3. **CrawlEventHandlers** - Events within the crawl iteration (before/after loading)

**Source File:** `pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/crawl/PageEvents.kt`

---

## PageEventHandlers Interface

```kotlin
interface PageEventHandlers {
    var loadEventHandlers: LoadEventHandlers
    var browseEventHandlers: BrowseEventHandlers
    var crawlEventHandlers: CrawlEventHandlers
    
    // Aliases for brevity
    var le // alias for loadEventHandlers
    var be // alias for browseEventHandlers
    var ce // alias for crawlEventHandlers
    
    fun chain(other: PageEventHandlers): PageEventHandlers
}
```

### Key Implementation Classes

| Class | Description |
|-------|-------------|
| `DefaultPageEventHandlers` | Default implementation with standard behaviors |
| `AbstractPageEventHandlers` | Base abstract implementation |

---

## LoadEventHandlers

Manages events related to the loading and parsing of web pages.

### Handler Reference Table

| Handler | Type | Signature | Description |
|---------|------|-----------|-------------|
| `onNormalize` | `UrlFilterEventHandler` | `(String) -> String?` | Fires when the URL is about to be normalized (e.g., remove fragment) |
| `onWillLoad` | `UrlEventHandler` | `(String) -> String?` | Fires when the URL is about to be loaded |
| `onWillFetch` | `WebPageEventHandler` | `(WebPage) -> Any?` | Fires when the URL is about to be fetched |
| `onFetched` | `WebPageEventHandler` | `(WebPage) -> Any?` | Fires when the URL has been fetched |
| `onWillParse` | `WebPageEventHandler` | `(WebPage) -> Any?` | Fires when the webpage is about to be parsed |
| `onWillParseHTMLDocument` | `WebPageEventHandler` | `(WebPage) -> Any?` | Fires when the HTML document is about to be parsed |
| `onHTMLDocumentParsed` | `HTMLDocumentEventHandler` | `(WebPage, FeaturedDocument) -> Any?` | Fires when the HTML document has been parsed |
| `onParsed` | `WebPageEventHandler` | `(WebPage) -> Any?` | Fires when the webpage parsing is complete |
| `onLoaded` | `WebPageEventHandler` | `(WebPage) -> Any?` | Fires when the webpage is fully loaded |

### Load Event Flow (Execution Order)

```
onNormalize → onWillLoad → onWillFetch → [Browser Phase] → onFetched → 
onWillParse → onWillParseHTMLDocument → onHTMLDocumentParsed → onParsed → onLoaded
```

### Load Event Handler Examples

```kotlin
loadEventHandlers.apply {
    // Normalize URLs (e.g., remove tracking parameters)
    onNormalize.addLast { url ->
        url.replace(Regex("\\?utm_.*"), "")
    }
    
    // Log before loading
    onWillLoad.addLast { url ->
        println("Loading: $url")
        url // return the url to continue, or null to skip
    }
    
    // Extract data after parsing
    onHTMLDocumentParsed.addLast { page: WebPage, document: FeaturedDocument ->
        val title = document.selectFirst("h1")?.text()
        println("Page title: $title")
    }
    
    // Final processing after load
    onLoaded.addLast { page ->
        println("Loaded ${page.url} with ${page.contentLength} bytes")
    }
}
```

---

## BrowseEventHandlers

Controls events during the interactive browsing phase when a real browser is used to render pages.

### Handler Reference Table

| Handler | Type | Signature | Description |
|---------|------|-----------|-------------|
| `onWillLaunchBrowser` | `WebPageEventHandler` | `(WebPage) -> Any?` | Fires when the browser is about to be launched |
| `onBrowserLaunched` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when the browser has been launched |
| `onWillFetch` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when the URL is about to be fetched via browser |
| `onFetched` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when the URL has been fetched via browser |
| `onWillNavigate` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when the URL is about to be navigated |
| `onNavigated` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when navigation is complete (like clicking Go button) |
| `onWillInteract` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when interaction with webpage is about to begin |
| `onWillCheckDocumentState` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when document state is about to be checked |
| `onDocumentFullyLoaded` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when document is fully loaded (custom algorithm, differs from Document.readyState) |
| `onWillScroll` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when page scrolling is about to begin |
| `onDidScroll` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when page scrolling has completed |
| `onDocumentSteady` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when document is steady (after scroll, good time for custom actions) |
| `onWillComputeFeature` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when webpage features are about to be computed |
| `onFeatureComputed` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when webpage features have been computed |
| `onDidInteract` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when all interactions have completed |
| `onWillStopTab` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when browser tab is about to be stopped |
| `onTabStopped` | `WebPageWebDriverEventHandler` | `suspend (WebPage, WebDriver) -> Any?` | Fires when browser tab has been stopped |

### Browse Event Flow (Execution Order)

```
onWillLaunchBrowser → onBrowserLaunched → onWillFetch → onWillNavigate → onNavigated → 
onWillInteract → onWillCheckDocumentState → onDocumentFullyLoaded → onWillScroll → 
onDidScroll → onDocumentSteady → onWillComputeFeature → onFeatureComputed → 
onDidInteract → onWillStopTab → onTabStopped → onFetched
```

### Important Notes on Browse Events

1. **`onDocumentSteady`** - This is the best event for custom interactions (clicking buttons, filling forms) because:
   - Document has finished loading
   - Initial scrolling is complete
   - Page content is stable
   - Fired before tab stops

2. **`onDocumentFullyLoaded`** - Different from standard `Document.readyState`:
   - Uses custom algorithm executed within the browser
   - More reliable for dynamic/AJAX content

3. **`WebPageWebDriverEventHandler`** - All browse handlers are suspend functions and receive both `WebPage` and `WebDriver` to enable RPA operations.

### Browse Event Handler Examples

```kotlin
browseEventHandlers.apply {
    // Warm up browser after launch
    onBrowserLaunched.addLast { page, driver ->
        driver.addInitScript("console.log('Browser initialized')")
    }
    
    // Wait for specific content after navigation
    onNavigated.addLast { page, driver ->
        driver.waitForSelector(".main-content", timeout = 10_000)
    }
    
    // Scroll to load lazy content
    onWillScroll.addLast { page, driver ->
        driver.scrollDown(5) // scroll down 5 times
    }
    
    // Click buttons or interact when document is stable
    onDocumentSteady.addLast { page, driver ->
        // Good time for custom RPA actions
        driver.click("button.load-more")
        driver.waitForNavigation()
    }
    
    // Capture screenshot before closing
    onWillStopTab.addLast { page, driver ->
        val screenshot = driver.captureScreenshot()
        // save screenshot...
    }
}
```

---

## CrawlEventHandlers

Manages events within the crawl iteration, occurring before and after the page loading process.

### Handler Reference Table

| Handler | Type | Signature | Description |
|---------|------|-----------|-------------|
| `onWillLoad` | `UrlAwareEventHandler` | `(UrlAware) -> UrlAware?` | Fires when URL is about to be loaded in crawl loop |
| `onLoaded` | `UrlAwareWebPageEventHandler` | `(UrlAware, WebPage?) -> Any?` | Fires when URL has been loaded in crawl loop |

### Crawl Event Flow

```
onWillLoad (crawl) → [Load Phase] → [Browse Phase] → onLoaded (crawl)
```

### Crawl Event Handler Examples

```kotlin
crawlEventHandlers.apply {
    // Filter or modify URLs before loading
    onWillLoad.addLast { url: UrlAware ->
        if (url.url.contains("skip-this")) {
            null // skip this URL
        } else {
            url // continue with URL
        }
    }
    
    // Process results after loading
    onLoaded.addLast { url, page ->
        if (page != null) {
            println("Successfully loaded: ${url.url}")
            // Process the loaded page
        } else {
            println("Failed to load: ${url.url}")
        }
    }
}
```

---

## Complete Event Lifecycle

The complete execution order of all events during a page load:

```
1.  crawl - onWillLoad
2.  load  - onNormalize
3.  load  - onWillLoad
4.  load  - onWillFetch
5.  browse - onWillLaunchBrowser
6.  browse - onBrowserLaunched
7.  browse - onWillFetch
8.  browse - onWillNavigate
9.  browse - onNavigated
10. browse - onWillInteract
11. browse - onWillCheckDocumentState
12. browse - onDocumentFullyLoaded
13. browse - onWillScroll
14. browse - onDidScroll
15. browse - onDocumentSteady
16. browse - onWillComputeFeature
17. browse - onFeatureComputed
18. browse - onDidInteract
19. browse - onWillStopTab
20. browse - onTabStopped
21. browse - onFetched
22. load  - onFetched
23. load  - onWillParse
24. load  - onWillParseHTMLDocument
25. load  - onHTMLDocumentParsed
26. load  - onParsed
27. load  - onLoaded
28. crawl - onLoaded
```

---

## Event Handler Types Reference

### Handler Type Definitions

| Type | Base Interface | Description |
|------|---------------|-------------|
| `UrlFilterEventHandler` | `AbstractChainedFunction1<String, String?>` | Filters/transforms URLs |
| `UrlEventHandler` | `AbstractChainedFunction1<String, String?>` | Handles URL events |
| `UrlAwareEventHandler` | `AbstractChainedFunction1<UrlAware, UrlAware>` | Handles UrlAware objects |
| `WebPageEventHandler` | `AbstractChainedFunction1<WebPage, Any?>` | Handles WebPage events |
| `HTMLDocumentEventHandler` | `AbstractChainedFunction2<WebPage, FeaturedDocument, Any?>` | Handles parsed document events |
| `UrlAwareWebPageEventHandler` | `AbstractChainedFunction2<UrlAware, WebPage?, Any?>` | Handles URL + page pairs |
| `WebPageWebDriverEventHandler` | `AbstractChainedPDFunction2<WebPage, WebDriver, Any?>` | Suspend handlers with WebDriver |

### Handler Methods

All handler types support these methods:

- `addFirst(handler)` - Add handler to the beginning of the chain
- `addLast(handler)` - Add handler to the end of the chain
- `removeFirst()` - Remove first handler
- `removeLast()` - Remove last handler
- `clear()` - Remove all handlers

---

## Usage Patterns

### Basic Usage with ListenableHyperlink

```kotlin
val session = PulsarContexts.createSession()
val handlers = DefaultPageEventHandlers()

handlers.loadEventHandlers.onLoaded.addLast { page ->
    println("Page loaded: ${page.url}")
}

val link = ListenableHyperlink(
    url = "https://example.com",
    args = "-refresh -parse",
    eventHandlers = handlers
)

session.submit(link)
PulsarContexts.await()
```

### Using LoadOptions with Event Handlers

```kotlin
val session = PulsarContexts.createSession()
val options = session.options("-refresh -parse")

options.eventHandlers.browseEventHandlers.onDocumentSteady.addLast { page, driver ->
    driver.click("#load-more-button")
    driver.waitForSelector(".new-content")
}

val page = session.load("https://example.com", options)
```

### Chaining Event Handlers

```kotlin
val baseHandlers = DefaultPageEventHandlers()
val customHandlers = DefaultPageEventHandlers()

// Configure custom handlers
customHandlers.loadEventHandlers.onLoaded.addLast { page ->
    println("Custom processing")
}

// Chain handlers - custom handlers execute after base handlers
baseHandlers.chain(customHandlers)
```

### Global Event Handlers

```kotlin
import ai.platon.pulsar.skeleton.crawl.GlobalEventHandlers

// Set global handlers that apply to all pages
GlobalEventHandlers.pageEventHandlers = DefaultPageEventHandlers().apply {
    loadEventHandlers.onLoaded.addLast { page ->
        println("Global: Page loaded")
    }
}
```

---

## Related Files

| File | Description |
|------|-------------|
| `PageEvents.kt` | Interface definitions for all event handler groups |
| `EventHandlers.kt` | Handler type definitions |
| `AbstractPageEvents.kt` | Abstract implementations |
| `PageEventDefaults.kt` | Default implementations |
| `GlobalEventHandlers.kt` | Global event handler singleton |
| `ListenableHyperlink.kt` | Hyperlink with event handler support |
| `LoadOptions.kt` | Options class containing eventHandlers |

---

## Quick Reference

### Adding a Handler

```kotlin
// Load events
handlers.loadEventHandlers.onLoaded.addLast { page -> /* ... */ }
handlers.le.onLoaded.addLast { page -> /* ... */ } // using alias

// Browse events  
handlers.browseEventHandlers.onDocumentSteady.addLast { page, driver -> /* ... */ }
handlers.be.onDocumentSteady.addLast { page, driver -> /* ... */ } // using alias

// Crawl events
handlers.crawlEventHandlers.onWillLoad.addLast { url -> url }
handlers.ce.onWillLoad.addLast { url -> url } // using alias
```

### Common Use Cases

| Use Case | Recommended Event |
|----------|-------------------|
| URL filtering/normalization | `loadEventHandlers.onNormalize` |
| Pre-fetch validation | `loadEventHandlers.onWillFetch` |
| Browser warmup | `browseEventHandlers.onBrowserLaunched` |
| Wait for content | `browseEventHandlers.onNavigated` |
| Custom RPA actions | `browseEventHandlers.onDocumentSteady` |
| Data extraction | `loadEventHandlers.onHTMLDocumentParsed` |
| Post-load processing | `loadEventHandlers.onLoaded` |
| Crawl result handling | `crawlEventHandlers.onLoaded` |

---

## See Also

- [Event Handling Guide](/docs/get-started/9event-handling.md) - Tutorial with examples
- [WebDriver Reference](/docs/get-started/11WebDriver.md) - WebDriver API documentation
- [RPA Guide](/docs/get-started/10RPA.md) - Robotic Process Automation guide
- [Concepts](/docs/concepts.md) - Core Browser4 concepts
