package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.FetchMode
import com.beust.jcommander.Parameter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object LoadOptionDefaults {
    var lazyFlush = true
    var parse = false
    var storeContent = true
}

/**
 * Created by vincent on 19-4-24.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * The expires field supports both ISO-8601 standard and hadoop time duration format
 * ISO-8601 standard : PnDTnHnMn.nS
 * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
 */
open class LoadOptions: CommonOptions {

    @Parameter(names = ["-l", "-label", "--label"], description = "The label of this load task")
    var label = ""

    @Parameter(names = ["-taskId", "--task-id"], description = "The task id. A task can contain multiple loadings")
    var taskId = LocalDate.now().toString()

    /**
     * TODO: should be a Instant
     * */
    @Parameter(names = ["-taskTime", "--task-time"],
            description = "The task time, we usually use a task time to indicate a batch of a task")
    var taskTime = LocalDate.now().toString()

    @Parameter(names = ["-authToken", "--auth-token"], description = "The auth token for this load task")
    var authToken = ""

    /**
     * Web page expiry time
     * The term "expires" usually be used for a expiry time, for example, http-equiv, or in cookie specification,
     * guess it means "expires at"
     * */
    @Parameter(names = ["-i", "-expires", "--expires"], converter = DurationConverter::class,
            description = "If a page is expired, it should be fetched from the internet again")
    var expires = ChronoUnit.CENTURIES.duration

    /** Web page expire time */
    @Parameter(names = ["-expireAt", "--expire-at"], converter = InstantConverter::class,
            description = "If a page is expired, it should be fetched from the internet again")
    var expireAt = Instant.EPOCH + ChronoUnit.CENTURIES.duration

    /** Arrange links */
    @Parameter(names = ["-ol", "-outLink", "-outLinkSelector", "--out-link-selector", "-outlink", "-outlinkSelector", "--outlink-selector"],
            description = "The CSS selector by which the anchors in the portal page are selected to load and analyze, " +
                    "Out pages will be detected automatically if the selector is empty")
    var outLinkSelector = ""

    @Parameter(names = ["-olp", "-outLinkPattern", "--out-link-pattern"], description = "The pattern of the out links")
    var outLinkPattern = ".+"

    @Parameter(names = ["-np", "-nextPage", "-nextPageSelector", "--next-page-selector"],
            description = "[TODO] The css selector of next page anchor")
    var nextPageSelector = ""

    @Parameter(names = ["-ifr", "-iframe", "--iframe"], description = "The i-th iframe")
    var iframe = 0

    @Parameter(names = ["-tl", "-topLinks", "--top-links"], description = "Top N links")
    var topLinks = 20

    @Parameter(names = ["-tng", "-topNAnchorGroups", "--top-anchor-groups"], description = "Try the top N anchor groups")
    var topNAnchorGroups = 3

    @Parameter(names = ["-wnb", "-waitNonBlank"],
            description = "[TODO] Wait for ajax content until the element is filled by a non-blank text")
    var waitNonBlank: String = ""

    @Parameter(names = ["-rnb", "-requireNotBlank"], description = "[TODO] Keep the pages only if the required text is not blank")
    var requireNotBlank: String = ""

    @Parameter(names = ["-rs", "-requireSize", "--require-size"], description = "Fetch pages smaller than requireSize in bytes")
    var requireSize = 0

    @Parameter(names = ["-ri", "-requireImages", "--require-images"], description = "Fetch pages who's images less than requireImages")
    var requireImages = 0

    @Parameter(names = ["-ra", "-requireAnchors", "--require-anchors"], description = "Fetch pages who's anchors less than requireAnchors")
    var requireAnchors = 0

    @Parameter(names = ["-fm", "-fetchMode", "--fetch-mode"], converter = FetchModeConverter::class,
            description = "The fetch mode, native, crowd sourcing and selenium are supported, selenium is the default")
    var fetchMode = FetchMode.BROWSER

    @Parameter(names = ["-b", "-browser", "--browser"], converter = BrowserTypeConverter::class,
            description = "The browser to use, google chrome is the default")
    var browser = BrowserType.CHROME

    @Parameter(names = ["-sc", "-scrollCount", "--scroll-count"],
            description = "The count to scroll down after a page is opened by a browser")
    var scrollCount = 3

    @Parameter(names = ["-si", "-scrollInterval", "--scroll-interval"], converter = DurationConverter::class,
            description = "The interval to scroll down after a page is opened by a browser")
    var scrollInterval = Duration.ofMillis(500)

    @Parameter(names = ["-stt", "-scriptTimeout", "--script-timeout"], converter = DurationConverter::class,
            description = "The maximum time to perform javascript injected into selenium")
    var scriptTimeout = Duration.ofSeconds(90)

    @Parameter(names = ["-plt", "-pageLoadTimeout", "--page-load-timeout"], converter = DurationConverter::class,
            description = "The maximum time to wait for a page to finish from the first http request start")
    var pageLoadTimeout = Duration.ofMinutes(3)

    // itemXXX should be available for all index-item pattern pages
    @Parameter(names = ["-ib", "-itemBrowser", "--item-browser"], converter = BrowserTypeConverter::class,
            description = "The browser used to visit the item pages, CHROME and NATIVE are supported")
    var itemBrowser = BrowserType.CHROME

    @Parameter(names = ["-ie", "-itemExtractor", "--item-extractor"], converter = BrowserTypeConverter::class,
            description = "The extract used to extract item pages, use BOILERPIPE for news and DEFAULT for others")
    var itemExtractor = ItemExtractor.DEFAULT

    @Parameter(names = ["-ii", "-itemExpires", "--item-expires"], converter = DurationConverter::class,
            description = "The same as expires, but only works for item pages in harvest tasks")
    var itemExpires = ChronoUnit.CENTURIES.duration

    /** Web page expire time */
    @Parameter(names = ["-itemExpireAt", "--item-expire-at"], converter = InstantConverter::class,
            description = "If a page is expired, it should be fetched from the internet again")
    var itemExpireAt = Instant.EPOCH + ChronoUnit.CENTURIES.duration

    /** Note: if scroll too many times, the page may fail to calculate the vision information */
    @Parameter(names = ["-isc", "-itemScrollCount", "--item-scroll-count"],
            description = "The same as scrollCount, but only works for item pages in harvest tasks")
    var itemScrollCount = scrollCount

    @Parameter(names = ["-isi", "-itemScrollInterval", "--item-scroll-interval"], converter = DurationConverter::class,
            description = "The same as scrollInterval, but only works for item pages in harvest tasks")
    var itemScrollInterval = scrollInterval

    @Parameter(names = ["-ist", "-itemScriptTimeout", "--item-script-timeout"], converter = DurationConverter::class,
            description = "The same as scriptTimeout, but only works for item pages in harvest tasks")
    var itemScriptTimeout = scriptTimeout

    @Parameter(names = ["-iplt", "-itemPageLoadTimeout", "--item-page-load-timeout"], converter = DurationConverter::class,
            description = "The same as pageLoadTimeout, but only works for item pages in harvest tasks")
    var itemPageLoadTimeout = pageLoadTimeout

    @Parameter(names = ["-irnb", "-itemRequireNotBlank", "--item-require-not-blank"],
            description = "Keep the item pages only if the required text is not blank")
    var itemRequireNotBlank = ""

    @Parameter(names = ["-irs", "-itemRequireSize", "--item-require-size"],
            description = "Fetch item pages smaller than requireSize")
    var itemRequireSize = 0

    @Parameter(names = ["-iri", "-itemRequireImages", "--item-require-images"],
            description = "Fetch item pages who's images less than requireImages")
    var itemRequireImages = 0

    @Parameter(names = ["-ira", "-itemRequireAnchors", "--item-require-anchors"],
            description = "Fetch item pages who's anchors less than requireAnchors")
    var itemRequireAnchors = 0

    /**
     * @deprecated shorten key is deprecated, use href instead
     * */
    @Parameter(names = ["-sk", "-shortenKey", "--shorten-key"],
            description = "Remove the query parameters when generate the page's key (reversed url)")
    var shortenKey = false

    @Parameter(names = ["-persist", "--persist"], arity = 1,
            description = "Persist fetched pages as soon as possible")
    var persist = true

    @Parameter(names = ["-storeContent", "--store-content"], arity = 1,
            description = "Persist page content into data store")
    var storeContent = LoadOptionDefaults.storeContent

    @Parameter(names = ["-cacheContent", "--cache-content"], arity = 1,
            description = "Cache the page content so it is still available after it be cleared for persistent")
    var cacheContent = false

    @Parameter(names = ["-retry", "--retry", "-retryFailed", "--retry-failed"],
            description = "Retry fetching the page if it's failed last time")
    var retryFailed = false

    @Parameter(names = ["-njr", "-nJitRetry", "--n-jit-retry"],
            description = "Retry at most n times if RETRY(1601) code return when fetching a page")
    var nJitRetry = 0

    @Parameter(names = ["-lazyFlush", "--lazy-flush"],
            description = "If false, flush persisted pages into database as soon as possible")
    var lazyFlush = LoadOptionDefaults.lazyFlush

    @Parameter(names = ["-preferParallel", "--prefer-parallel"], arity = 1,
            description = "Parallel fetch pages whenever applicable")
    var preferParallel = true

    @Parameter(names = ["-ic", "-incognito", "--incognito"], description = "Run browser in incognito mode")
    var incognito = false

    @Parameter(names = ["-background", "--background"], description = "Fetch the page in background")
    var background: Boolean = false

    @Parameter(names = ["-noRedirect", "--no-redirect"], description = "Do not redirect")
    var noRedirect = false

    @Parameter(names = ["-hardRedirect", "--hard-redirect"],
            description = "If false, return the original page record but the redirect target's content, " +
                    "otherwise, return the page record of the redirected target")
    var hardRedirect = false

    // parse options
    @Parameter(names = ["-ps", "-parse", "--parse"], description = "Parse the page after fetch")
    var parse = LoadOptionDefaults.parse

    @Parameter(names = ["-rpl", "-reparseLinks", "--reparse-links"], description = "Re-parse all links if the page is parsed")
    var reparseLinks = false

    @Parameter(names = ["-ignoreUrlQuery", "--ignore-url-query"], description = "Remove the query parameters of urls")
    var ignoreUrlQuery = false

    @Parameter(names = ["-noNorm", "--no-link-normalizer"], description = "No normalizer is applied to parse links")
    var noNorm = false

    @Parameter(names = ["-noFilter", "--no-link-filter"], description = "No filter is applied to parse links")
    var noFilter = false

    @Parameter(names = ["-q", "-query", "--query"], description = "Extract query to extract data from")
    var query: String? = null

    @Parameter(names = ["-m", "-withModel", "--with-model"], description = "Also load page model when loading a page")
    var withModel = false

    @Parameter(names = ["-lk", "-withLinks", "--with-links"], description = "Contains links when loading page model")
    var withLinks = false

    @Parameter(names = ["-tt", "-withText", "--with-text"], description = "Contains text when loading page model")
    var withText = false

    // A volatile config is usually in session scope
    var volatileConfig: VolatileConfig? = null

    /**
     * If shortenKey is set, also ignore url query when fetch pages
     * */
    val ignoreQuery get() = shortenKey

    open val modifiedParams: Params
        get() {
            val rowFormat = "%40s: %s"
            val fields = LoadOptions::class.java.declaredFields
            return fields.filter { it.annotations.any { it is Parameter } && !isDefault(it.name) }
                    .onEach { it.isAccessible = true }
                    .filter { it.get(this) != null }
                    .associate { "-${it.name}" to it.get(this) }
                    .let { Params.of(it).withRowFormat(rowFormat) }
        }

    open val modifiedOptions: Map<String, Any>
        get() {
            val fields = LoadOptions::class.java.declaredFields
            return fields.filter { it.annotations.any { it is Parameter } && !isDefault(it.name) }
                    .onEach { it.isAccessible = true }
                    .filter { it.get(this) != null }
                    .associate { it.name to it.get(this) }
        }

    protected constructor() {
        addObjects(this)
    }

    protected constructor(argv: Array<String>) : super(argv) {
        addObjects(this)
    }

    protected constructor(args: String) : super(args) {
        addObjects(this)
    }

    /**
     * Parse with parameter overwriting fix
     * */
    override fun parse(): Boolean {
        val b = super.parse()
        if (b) {
            LoadOptions::class.java.declaredFields.asSequence()
                .filter { arity0BooleanParams.contains("-${it.name}") }
                .filter { argv.contains("-${it.name}") }
                .forEach {
                    it.isAccessible = true
                    it.set(this, true)
                }
        }
        return b
    }

    open fun createItemOptions(conf: VolatileConfig? = null): LoadOptions {
        val itemOptions = clone()
        itemOptions.itemOptions2MajorOptions()

        if (itemOptions.browser == BrowserType.NATIVE) {
            itemOptions.fetchMode = FetchMode.NATIVE
        }

        itemOptions.volatileConfig = conf ?: volatileConfig

        return itemOptions
    }

    fun isExpired(time: Instant): Boolean {
        return time > expireAt || time + expires < Instant.now()
    }

    open fun itemOptions2MajorOptions() {
        expires = itemExpires
        scrollCount = itemScrollCount
        scriptTimeout = itemScriptTimeout
        scrollInterval = itemScrollInterval
        pageLoadTimeout = itemPageLoadTimeout
        requireNotBlank = itemRequireNotBlank
        requireSize = itemRequireSize
        requireImages = itemRequireImages
        requireAnchors = itemRequireAnchors
        browser = itemBrowser
    }

    fun apply(conf: VolatileConfig?): VolatileConfig? = conf?.apply {
        setInt(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, scrollCount)
        setDuration(CapabilityTypes.FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        setDuration(CapabilityTypes.FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        setDuration(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
        setBoolean(CapabilityTypes.BROWSER_INCOGNITO, incognito)
    }

    open fun isDefault(option: String): Boolean {
        val value = LoadOptions::class.java.declaredFields.find { it.name == option }
                ?.also { it.isAccessible = true }?.get(this) ?: return false
        return value == defaultParams[option]
    }

    override fun getParams(): Params {
        val rowFormat = "%40s: %s"
        val fields = LoadOptions::class.java.declaredFields
        return fields.filter { it.annotations.any { it is Parameter } }
                .onEach { it.isAccessible = true }
                .associate { "-${it.name}" to it.get(this) }
                .filter { it.value != null }
                .let { Params.of(it).withRowFormat(rowFormat) }
    }

    override fun toString(): String {
        return modifiedParams.distinct().sorted()
                .withCmdLineStyle(true)
                .withKVDelimiter(" ")
                .withDistinctBooleanParams(arity1BooleanParams)
                .formatAsLine().replace("\\s+".toRegex(), " ")
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        return other is LoadOptions && other.toString() == toString()
    }

    // TODO: can not rely on any member filed because static filed defaultParams uses hashCode but none of the fields is initialized
    override fun hashCode(): Int {
        return super.hashCode()
    }

    /**
     * Create a new LoadOptions
     * */
    open fun clone(): LoadOptions {
        return parse(toString(), volatileConfig)
    }

    companion object {
        val default = LoadOptions()
        val defaultParams = LoadOptions::class.java.declaredFields.associate { it.name to it.get(default) }
        val defaultArgsMap = default.toArgsMap()
        val arity0BooleanParams = LoadOptions::class.java.declaredFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .filter { it.get(default) is Boolean }
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .filter { it.arity < 1 }
            .flatMap { it.names.toList() }
            .toList()
        val arity1BooleanParams = LoadOptions::class.java.declaredFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .filter { it.get(default) is Boolean }
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .filter { it.arity == 1 }
            .flatMap { it.names.toList() }
            .toList()
        val optionNames = LoadOptions::class.java.declaredFields
            .asSequence()
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .flatMap { it.names.toList() }
            .toList()

        val helpList: List<List<String>> get() =
                LoadOptions::class.java.declaredFields
                    .asSequence()
                    .mapNotNull { (it.annotations.firstOrNull { it is Parameter } as? Parameter)?.to(it) }
                    .map {
                        listOf(it.first.names.joinToString { it },
                                it.second.type.typeName.substringAfterLast("."),
                                defaultParams[it.second.name].toString(),
                                it.first.description
                        )
                    }.toList()

        fun setFieldByAnnotation(options: LoadOptions, annotationName: String, value: Any) {
            LoadOptions::class.java.declaredFields.forEach {
                val found = it.annotations.filterIsInstance<Parameter>().any { annotationName in it.names }
                if (found) {
                    it.isAccessible = true
                    it.set(options, value)
                }
            }
        }

        @JvmOverloads
        fun create(volatileConfig: VolatileConfig? = null): LoadOptions {
            val options = LoadOptions()
            options.parse()
            options.volatileConfig = volatileConfig
            return options
        }

        @JvmOverloads
        fun parse(args: String, volatileConfig: VolatileConfig? = null): LoadOptions {
            val options = LoadOptions(args.trim())
            options.parse()
            options.volatileConfig = volatileConfig
            return options
        }

        /**
         * Create a new LoadOptions with o1 and o2's items, o2 overrides o1
         * */
        @JvmOverloads
        fun merge(o1: LoadOptions, o2: LoadOptions, volatileConfig: VolatileConfig? = null): LoadOptions {
            return parse("$o1 $o2", volatileConfig)
        }

        @JvmOverloads
        fun merge(args: String?, args2: String?, volatileConfig: VolatileConfig? = null): LoadOptions {
            return parse("$args $args2", volatileConfig)
        }
    }
}
