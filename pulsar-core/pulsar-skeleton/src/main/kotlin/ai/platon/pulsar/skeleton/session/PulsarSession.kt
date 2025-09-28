package ai.platon.pulsar.skeleton.session

import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.extractor.TextDocument
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.ai.WebDriverAgent
import ai.platon.pulsar.skeleton.ai.tta.ActionDescription
import ai.platon.pulsar.skeleton.ai.tta.ActionOptions
import ai.platon.pulsar.skeleton.ai.tta.InstructionResult
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.common.urls.NormURL
import ai.platon.pulsar.skeleton.context.PulsarContext
import ai.platon.pulsar.skeleton.crawl.PageEventHandlers
import ai.platon.pulsar.skeleton.crawl.common.DocumentCatch
import ai.platon.pulsar.skeleton.crawl.common.GlobalCache
import ai.platon.pulsar.skeleton.crawl.common.PageCatch
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.google.common.annotations.Beta
import org.jsoup.nodes.Element
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * PulsarSession is the primary high‑level façade for interacting with Browser4/Pulsar.
 * It centralizes URL normalization, page loading (from cache or network), parsing, extracting, scraping,
 * exporting, submitting URLs to the crawl loop, and limited AI / browser‑automation helpers.
 *
 * Responsibilities (summary):
 *  - Normalize raw URLs and apply/merge [LoadOptions]
 *  - Retrieve pages from local storage (page store) or fetch them (HTTP or real browser)
 *  - Decide (via options) whether a page needs refreshing (expiration / required fields / min size / force)
 *  - Parse pages into [FeaturedDocument] (HTML DOM + convenience utilities)
 *  - Extract or scrape structured fields via CSS selectors
 *  - Submit URLs (deferred processing) vs. Load (immediate synchronous acquisition)
 *  - Batch versions for loading, submitting, or scraping out‑links selected by an option
 *  - Resource loading (lightweight, no full browser rendering) with an optional referrer environment
 *  - Export and persist pages / documents to disk or storage
 *  - Integrate with a bound [WebDriver] / [Browser] for interactive or JS‑heavy pages
 *  - Provide AI/LLM based assistance (chat / act / instruct) over pages, documents, or elements
 *
 * Loading Strategy (simplified flow):
 *  1. Normalize URL + apply resolved [LoadOptions].
 *  2. Check local storage & caches (page/document caches if enabled).
 *  3. If a valid, non‑expired, requirements‑satisfying page is found -> reuse.
 *  4. Else fetch (HTTP or real browser depending on options / handler chain).
 *  5. Persist / cache according to policy.
 *
 * "NIL page" Mentioned Throughout:
 *  Some methods never return null; if a page does not exist they return a sentinel placeholder ("NIL").
 *  Methods suffixed with `OrNull` can return null instead. Always check the specific signature.
 *
 * Performance & Caching:
 *  - Page cache + document cache can be disabled via [disablePDCache].
 *  - Normalization reduces duplicate fetches by canonicalizing equivalent URLs.
 *
 * Concurrency & Async Models:
 *  - Kotlin suspend variants (…Deferred) provide structured concurrency integration.
 *  - Java style async variants (…Async) return [CompletableFuture].
 *  - Submit APIs are non‑blocking; processing occurs later in the main crawl loop.
 *
 * Browser / Automation:
 *  - Bind a driver/browser with [bindDriver]/[bindBrowser] so later actions use them implicitly.
 *  - Attach an existing driver context to a page without navigating using [attach].
 *  - Use [PageEventHandlers] in options to inject scripted interactions (e.g. fill form, click, scroll).
 *
 * AI Integration:
 *  - Chat over raw prompts, pages, documents, or elements.
 *  - High‑level action generation: [act], [instruct] (experimental / @Beta components apply).
 *
 * Scraping Helpers:
 *  - [scrape] loads + parses + extracts fields in one step.
 *  - Out‑page variants ([loadOutPages], [scrapeOutPages], [submitForOutPages]) leverage the `-outLink` option
 *    to select links from a portal page.
 *
 * Examples (basic):
 *  ```kotlin
 *  val session = /* created via PulsarContexts */
 *  val page = session.load("https://example.com", "-expire 1d")
 *  val doc = session.parse(page)
 *  val title = doc.selectFirstTextOrNull("title")
 *  val fields = session.scrape("https://example.com", "-expire 1d", listOf(".title", ".content"))
 *  session.submit("https://example.com/list", "-outLink a.item -expire 6h")
 *  ```
 *
 * See also grouped methods for batch operations and async/suspend variants.
 *
 * @see UrlAware              Abstraction that couples a URL and per‑URL options.
 * @see LoadOptions           Declarative load / fetch / parse / event configuration.
 * @see WebPage               Persistent representation of a fetched page.
 * @see FeaturedDocument      Parsed HTML DOM wrapper.
 * @see PageEventHandlers     Lifecycle hooks for browser & parsing events.
 * @see WebDriver             Automation interface for dynamic pages.
 */
interface PulsarSession : AutoCloseable {
    /** Unique numeric session identifier. */
    val id: Int
    /** Whether the session is still active (not closed). */
    val isActive: Boolean
    /** Context that created this session; shared services & infrastructure. */
    val context: PulsarContext
    /** Immutable process‑level configuration snapshot (loaded at startup, never mutated). */
    val unmodifiedConfig: ImmutableConfig
    /**
     * Session‑scoped mutable configuration allowing dynamic runtime tuning.
     * Prefer using this over global config for per‑session overrides.
     */
    val sessionConfig: VolatileConfig
    /** Short human‑readable descriptor (for logs / diagnostics). */
    val display: String
    /** Global page cache (lookup by normalized URL). */
    val pageCache: PageCatch
    /** Global parsed document cache. */
    val documentCache: DocumentCatch
    /** Miscellaneous global caches (shared). */
    val globalCache: GlobalCache
    /** Currently bound webdriver (if any). Subsequent driver‑dependent calls reuse it automatically. */
    val boundDriver: WebDriver?
    /** Currently bound browser (if any). Subsequent browser‑dependent calls reuse it automatically. */
    val boundBrowser: Browser?

    /** Disable both page and document caches for this session (subsequent operations). */
    fun disablePDCache()

    /** Retrieve arbitrary session data previously stored via the two‑argument [data] variant. */
    fun data(name: String): Any?
    /** Store arbitrary session data (thread‑safe usage depends on implementation). */
    fun data(name: String, value: Any)
    /** Get a string property from the session scope (lightweight key/value). */
    fun property(name: String): String?
    /** Set a string property in the session scope. */
    fun property(name: String, value: String)

    /** Create new [LoadOptions] derived from an argument DSL string. */
    fun options(args: String = ""): LoadOptions
    /** Create new [LoadOptions] from DSL string plus explicit [eventHandlers]. */
    fun options(args: String = "", eventHandlers: PageEventHandlers?): LoadOptions
    /** Normalize / finalize a [LoadOptions] instance (fill defaults / canonicalize). */
    fun normalize(options: LoadOptions): LoadOptions

    // --------------------------------------------------------------------------------------------
    // URL Normalization
    // --------------------------------------------------------------------------------------------
    /** Normalize a raw URL string -> [NormURL]. */
    fun normalize(url: String): NormURL
    /** Normalize with argument DSL; optionally convert global options to per‑item options. */
    fun normalize(url: String, args: String, toItemOption: Boolean = false): NormURL
    /** Normalize with a pre‑built [LoadOptions]. */
    fun normalize(url: String, options: LoadOptions, toItemOption: Boolean = false): NormURL
    /** Nullable variant; returns null when invalid instead of throwing / returning sentinel. */
    fun normalizeOrNull(url: String?, options: LoadOptions = options(), toItemOption: Boolean = false): NormURL?

    /** Bulk normalization of raw URL strings; invalid URLs are filtered out. */
    fun normalize(urls: Iterable<String>): List<NormURL>
    /** Bulk normalization with argument DSL; invalid URLs removed. */
    fun normalize(urls: Iterable<String>, args: String, toItemOption: Boolean = false): List<NormURL>
    /** Bulk normalization with shared [LoadOptions]; invalid URLs removed. */
    fun normalize(urls: Iterable<String>, options: LoadOptions, toItemOption: Boolean = false): List<NormURL>

    /** Normalize a [UrlAware] (already decorated URL); returns [NormURL.NIL] if invalid. */
    fun normalize(url: UrlAware): NormURL
    /** Normalize with argument DSL. */
    fun normalize(url: UrlAware, args: String, toItemOption: Boolean = false): NormURL
    /** Normalize with custom [LoadOptions]. */
    fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean = false): NormURL
    /** Nullable variant for [UrlAware]. */
    fun normalizeOrNull(url: UrlAware?, options: LoadOptions = options(), toItemOption: Boolean = false): NormURL?

    /** Bulk normalization for decorated URLs. */
    fun normalize(urls: Collection<UrlAware>): List<NormURL>
    /** Bulk normalization (argument DSL). */
    fun normalize(urls: Collection<UrlAware>, args: String, toItemOption: Boolean = false): List<NormURL>
    /** Bulk normalization (shared options). */
    fun normalize(urls: Collection<UrlAware>, options: LoadOptions, toItemOption: Boolean = false): List<NormURL>

    // --------------------------------------------------------------------------------------------
    // Local Storage Retrieval (No Network Fetch)
    // --------------------------------------------------------------------------------------------
    /** Get a page from local storage (returns NIL sentinel if missing). */
    fun get(url: String): WebPage
    /** Get a page from local storage with a subset of requested field names (lazy load). */
    fun get(url: String, vararg fields: String): WebPage
    /** Get a page from local storage or null if absent. */
    fun getOrNull(url: String): WebPage?
    /** Get a page from local storage requesting specific fields or null if absent. */
    fun getOrNull(url: String, vararg fields: String): WebPage?
    /** Retrieve raw content as ByteBuffer (null if missing / content absent). */
    fun getContent(url: String): ByteBuffer?
    /** Retrieve raw content as a UTF‑8 (or detected) String (experimental / may allocate). */
    @Beta fun getContentAsString(url: String): String?
    /** True if a locally stored page record exists (does not guarantee freshness). */
    fun exists(url: String): Boolean

    /** Determine (without side effects) the fetch/check state of a page under given options. */
    fun fetchState(page: WebPage, options: LoadOptions): CheckState

    // --------------------------------------------------------------------------------------------
    // Immediate Open (Forced Navigation)
    // --------------------------------------------------------------------------------------------
    /** Force open (navigate & fetch) a URL ignoring local freshness heuristics. */
    fun open(url: String): WebPage
    /** Force open with custom event handlers for this single navigation. */
    fun open(url: String, eventHandlers: PageEventHandlers): WebPage
    /** Suspend variant using supplied [WebDriver]. */
    suspend fun open(url: String, driver: WebDriver): WebPage
    /** Suspend variant with both driver and ad‑hoc handlers. */
    suspend fun open(url: String, driver: WebDriver, eventHandlers: PageEventHandlers): WebPage

    /** Attach current driver DOM / state to a synthetic page object (no navigation). */
    suspend fun attach(url: String, driver: WebDriver): WebPage
    /** Attach with event handlers. */
    suspend fun attach(url: String, driver: WebDriver, eventHandlers: PageEventHandlers): WebPage

    /** Bind a webdriver to the session for implicit reuse. */
    fun bindDriver(driver: WebDriver)
    /** Bind a browser to the session for implicit reuse. */
    fun bindBrowser(browser: Browser)

    // --------------------------------------------------------------------------------------------
    // Load (Conditional Fetch)
    // --------------------------------------------------------------------------------------------
    /** Load a URL (reuse cached copy if valid; otherwise fetch). */
    fun load(url: String): WebPage
    /** Load with argument DSL (e.g. "-expire 1d -requireSize 1000"). */
    fun load(url: String, args: String): WebPage
    /** Load with explicit [LoadOptions]. */
    fun load(url: String, options: LoadOptions): WebPage
    /** Load a decorated URL ([UrlAware]). */
    fun load(url: UrlAware): WebPage
    /** Load a decorated URL with argument DSL. */
    fun load(url: UrlAware, args: String): WebPage
    /** Load a decorated URL with explicit options. */
    fun load(url: UrlAware, options: LoadOptions): WebPage
    /** Load a normalized URL. */
    fun load(url: NormURL): WebPage

    /** Suspend variant: load with argument DSL. */
    suspend fun loadDeferred(url: String, args: String): WebPage
    /** Suspend variant: load with options (defaults if omitted). */
    suspend fun loadDeferred(url: String, options: LoadOptions = options()): WebPage
    /** Suspend variant: decorated URL + argument DSL. */
    suspend fun loadDeferred(url: UrlAware, args: String): WebPage
    /** Suspend variant: decorated URL + options. */
    suspend fun loadDeferred(url: UrlAware, options: LoadOptions = options()): WebPage
    /** Suspend variant: normalized URL. */
    suspend fun loadDeferred(url: NormURL): WebPage

    // --------------------------------------------------------------------------------------------
    // Batch Load (Immediate) - Failing URLs are skipped, successful pages returned
    // --------------------------------------------------------------------------------------------
    fun loadAll(urls: Iterable<String>): List<WebPage>
    fun loadAll(urls: Iterable<String>, args: String): List<WebPage>
    fun loadAll(urls: Iterable<String>, options: LoadOptions): List<WebPage>
    fun loadAll(urls: Collection<UrlAware>): List<WebPage>
    fun loadAll(urls: Collection<UrlAware>, args: String): List<WebPage>
    fun loadAll(urls: Collection<UrlAware>, options: LoadOptions): List<WebPage>
    fun loadAll(normUrls: List<NormURL>): List<WebPage>

    // --------------------------------------------------------------------------------------------
    // Java CompletableFuture Async Variants
    // --------------------------------------------------------------------------------------------
    fun loadAsync(url: String): CompletableFuture<WebPage>
    fun loadAsync(url: String, args: String): CompletableFuture<WebPage>
    fun loadAsync(url: String, options: LoadOptions): CompletableFuture<WebPage>
    fun loadAsync(url: UrlAware): CompletableFuture<WebPage>
    fun loadAsync(url: UrlAware, args: String): CompletableFuture<WebPage>
    fun loadAsync(url: UrlAware, options: LoadOptions): CompletableFuture<WebPage>
    fun loadAsync(url: NormURL): CompletableFuture<WebPage>

    fun loadAllAsync(urls: Iterable<String>): List<CompletableFuture<WebPage>>
    fun loadAllAsync(urls: Iterable<String>, args: String): List<CompletableFuture<WebPage>>
    fun loadAllAsync(urls: Iterable<String>, options: LoadOptions): List<CompletableFuture<WebPage>>
    fun loadAllAsync(urls: Collection<UrlAware>): List<CompletableFuture<WebPage>>
    fun loadAllAsync(urls: Collection<UrlAware>, args: String): List<CompletableFuture<WebPage>>
    fun loadAllAsync(urls: Collection<UrlAware>, options: LoadOptions): List<CompletableFuture<WebPage>>
    fun loadAllAsync(urls: List<NormURL>): List<CompletableFuture<WebPage>>

    // --------------------------------------------------------------------------------------------
    // Submit (Deferred Processing) - Non‑blocking
    // --------------------------------------------------------------------------------------------
    fun submit(url: String): PulsarSession
    fun submit(url: String, args: String): PulsarSession
    /** Submit with explicit options (handlers may be embedded). */
    fun submit(url: String, options: LoadOptions): PulsarSession

    fun submit(url: UrlAware): PulsarSession
    fun submit(url: UrlAware, args: String): PulsarSession
    /**
     * Intentionally not implemented: would be ambiguous for event handler resolution.
     * Throws immediately when invoked.
     */
    fun submit(url: UrlAware, options: LoadOptions): PulsarSession =
        throw NotImplementedError("submit(UrlAware, LoadOptions) is intentionally omitted due to event complexity.")

    fun submitAll(urls: Iterable<String>): PulsarSession
    fun submitAll(urls: Iterable<String>, args: String): PulsarSession
    fun submitAll(urls: Iterable<String>, options: LoadOptions): PulsarSession
    fun submitAll(urls: Collection<UrlAware>): PulsarSession
    fun submitAll(urls: Collection<UrlAware>, args: String): PulsarSession
    /** Not implemented (see single URL explanation). */
    fun submitAll(urls: Collection<UrlAware>, options: LoadOptions): PulsarSession =
        throw NotImplementedError("submitAll(Collection<UrlAware>, LoadOptions) omitted due to event complexity.")

    // --------------------------------------------------------------------------------------------
    // Out Pages (Portal + Selected Links)
    // --------------------------------------------------------------------------------------------
    /** Not implemented – would lack necessary option context. */
    fun loadOutPages(portalUrl: String): List<WebPage> =
        throw NotImplementedError("loadOutPages(String) requires explicit -outLink selection and is omitted.")

    fun loadOutPages(portalUrl: String, args: String): List<WebPage>
    fun loadOutPages(portalUrl: String, options: LoadOptions): List<WebPage>
    /** Not implemented – requires explicit control of events/options. */
    fun loadOutPages(portalUrl: UrlAware): List<WebPage> =
        throw NotImplementedError("loadOutPages(UrlAware) omitted (ambiguous event handling).")
    fun loadOutPages(portalUrl: UrlAware, args: String): List<WebPage>
    fun loadOutPages(portalUrl: UrlAware, options: LoadOptions): List<WebPage>
    /** Not implemented – normalized variant adds no value without options. */
    fun loadOutPages(portalUrl: NormURL): List<WebPage> =
        throw NotImplementedError("loadOutPages(NormURL) omitted (must specify options / events).")

    fun loadOutPagesAsync(portalUrl: String, args: String): List<CompletableFuture<WebPage>>
    fun loadOutPagesAsync(portalUrl: String, options: LoadOptions): List<CompletableFuture<WebPage>>

    fun submitForOutPages(portalUrl: String, args: String): PulsarSession
    fun submitForOutPages(portalUrl: String, options: LoadOptions): PulsarSession
    fun submitForOutPages(portalUrl: UrlAware, args: String): PulsarSession
    fun submitForOutPages(portalUrl: UrlAware, options: LoadOptions): PulsarSession

    // --------------------------------------------------------------------------------------------
    // Resource Loading (Lightweight, no full browser render of the target resource URL itself)
    // --------------------------------------------------------------------------------------------
    fun loadResource(url: String, referrer: String): WebPage
    fun loadResource(url: String, referrer: String, args: String): WebPage
    fun loadResource(url: String, referrer: String, options: LoadOptions): WebPage
    suspend fun loadResourceDeferred(url: String, referrer: String): WebPage
    suspend fun loadResourceDeferred(url: String, referrer: String, args: String): WebPage
    suspend fun loadResourceDeferred(url: String, referrer: String, options: LoadOptions): WebPage

    // --------------------------------------------------------------------------------------------
    // Parsing & Document Loading
    // --------------------------------------------------------------------------------------------
    fun parse(page: WebPage): FeaturedDocument
    fun parse(page: WebPage, noCache: Boolean): FeaturedDocument

    fun loadDocument(url: String): FeaturedDocument
    fun loadDocument(url: String, args: String): FeaturedDocument
    fun loadDocument(url: String, options: LoadOptions): FeaturedDocument
    fun loadDocument(url: UrlAware): FeaturedDocument
    fun loadDocument(url: UrlAware, args: String): FeaturedDocument
    fun loadDocument(url: UrlAware, options: LoadOptions): FeaturedDocument
    fun loadDocument(url: NormURL): FeaturedDocument

    // --------------------------------------------------------------------------------------------
    // Extraction (Operate on already parsed [FeaturedDocument])
    // --------------------------------------------------------------------------------------------
    fun extract(document: FeaturedDocument, fieldSelectors: Iterable<String>): Map<String, String?>
    fun extract(document: FeaturedDocument, restrictSelector: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>
    fun extract(document: FeaturedDocument, fieldSelectors: Map<String, String>): Map<String, String?>
    fun extract(document: FeaturedDocument, restrictSelector: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>

    // --------------------------------------------------------------------------------------------
    // Scrape (Load + Parse + Extract convenience)
    // --------------------------------------------------------------------------------------------
    fun scrape(url: String, args: String, fieldSelectors: Iterable<String>): Map<String, String?>
    fun scrape(url: String, options: LoadOptions, fieldSelectors: Iterable<String>): Map<String, String?>
    fun scrape(url: String, args: String, fieldSelectors: Map<String, String>): Map<String, String?>
    fun scrape(url: String, options: LoadOptions, fieldSelectors: Map<String, String>): Map<String, String?>
    fun scrape(url: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>
    fun scrape(url: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>
    fun scrape(url: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>
    fun scrape(url: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>

    // --------------------------------------------------------------------------------------------
    // Out Page Scraping (Portal + Out links)
    // --------------------------------------------------------------------------------------------
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>
    fun scrapeOutPages(portalUrl: String, options: LoadOptions, fieldSelectors: Iterable<String>): List<Map<String, String?>>
    fun scrapeOutPages(portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>
    fun scrapeOutPages(portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Iterable<String>): List<Map<String, String?>>
    fun scrapeOutPages(portalUrl: String, args: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>
    fun scrapeOutPages(portalUrl: String, options: LoadOptions, fieldSelectors: Map<String, String>): List<Map<String, String?>>
    fun scrapeOutPages(portalUrl: String, args: String, restrictSelector: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>
    fun scrapeOutPages(portalUrl: String, options: LoadOptions, restrictSelector: String, fieldSelectors: Map<String, String>): List<Map<String, String?>>

    // --------------------------------------------------------------------------------------------
    // Content Harvesting (Boilerplate Removal / Text Extraction Engines)
    // --------------------------------------------------------------------------------------------
    fun harvest(url: String, args: String = "", engine: String = "boilerpipe"): TextDocument
    fun harvest(page: WebPage, engine: String = "boilerpipe"): TextDocument

    // --------------------------------------------------------------------------------------------
    // AI / LLM Chat & Action
    // --------------------------------------------------------------------------------------------
    fun chat(prompt: String): ModelResponse
    fun chat(userMessage: String, systemMessage: String): ModelResponse
    fun chat(page: WebPage, prompt: String): ModelResponse
    fun chat(prompt: String, page: WebPage): ModelResponse
    fun chat(document: FeaturedDocument, prompt: String): ModelResponse
    fun chat(prompt: String, document: FeaturedDocument): ModelResponse
    fun chat(element: Element, prompt: String): ModelResponse
    fun chat(prompt: String, element: Element): ModelResponse

    /** Generate and execute exactly one webdriver action inferred from natural language. */
    suspend fun act(prompt: String): InstructionResult
    @Beta suspend fun act(action: ActionOptions): WebDriverAgent
    /** Execute a structured action description (already resolved). */
    suspend fun act(action: ActionDescription): InstructionResult
    /** Multi‑step natural language instruction -> sequence of webdriver actions. */
    suspend fun instruct(prompt: String): InstructionResult

    // --------------------------------------------------------------------------------------------
    // Export (Serialization / Archival) & Persistence
    // --------------------------------------------------------------------------------------------
    fun export(page: WebPage): Path
    fun export(page: WebPage, ident: String = ""): Path
    fun exportTo(page: WebPage, path: Path): Path
    fun export(doc: FeaturedDocument): Path
    fun export(doc: FeaturedDocument, ident: String = ""): Path
    fun exportTo(doc: FeaturedDocument, path: Path): Path

    fun persist(page: WebPage): Boolean
    fun delete(url: String)
    /** Flush buffered changes (e.g. batched persistence) to durable storage. */
    fun flush()
}
