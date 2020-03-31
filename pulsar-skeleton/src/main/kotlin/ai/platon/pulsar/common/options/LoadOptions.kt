package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.FetchMode
import com.beust.jcommander.Parameter
import java.time.Duration

/**
 * Created by vincent on 19-4-24.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * The expires field supports both ISO-8601 standard and hadoop time duration format
 * ISO-8601 standard : PnDTnHnMn.nS
 * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
 */
open class LoadOptions: CommonOptions {
    /** Fetch */
    @Parameter(names = ["-i", "-expires", "--expires"], converter = DurationConverter::class,
            description = "If a page is expired, it should be fetched from the internet again")
    var expires = Duration.ofDays(36500)
    @Parameter(names = ["-ic", "-incognito", "--incognito"], description = "Simulate browser as incognito mode")
    var incognito = false

    /** Arrange links */
    @Parameter(names = ["-ol", "-outlink", "-outlinkSelector", "--outlink-selector"],
            description = "The CSS selector by which the anchors in the portal page are selected to load and analyze, " +
                    "Out pages will be detected automatically if the selector is empty")
    var outlinkSelector = ""
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
    @Parameter(names = ["-rs", "-requireSize", "--require-size"], description = "Fetch pages smaller than requireSize")
    var requireSize = 0
    @Parameter(names = ["-ri", "-requireImages", "--require-images"], description = "Fetch pages who's images less than requireImages")
    var requireImages = 0
    @Parameter(names = ["-ra", "-requireAnchors", "--require-anchors"], description = "Fetch pages who's anchors less than requireAnchors")
    var requireAnchors = 0

    @Parameter(names = ["-fm", "-fetchMode", "--fetch-mode"], converter = FetchModeConverter::class,
            description = "The fetch mode, native, crowd sourcing and selenium are supported, selenium is the default")
    var fetchMode = FetchMode.SELENIUM
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
    var itemExpires = Duration.ofDays(36500)
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

    @Parameter(names = ["-sk", "-shortenKey", "--shorten-key"],
            description = "Remove the query parameters when generate the page's key (reversed url)")
    var shortenKey = false
    @Parameter(names = ["-persist", "--persist"], arity = 1,
            description = "Persist fetched pages as soon as possible")
    var persist = true

    @Parameter(names = ["-retry", "--retry"],
            description = "Retry fetching the page if it's failed last time")
    var retryFailed = false
    @Parameter(names = ["-lazyFlush", "--lazy-flush"],
            description = "If false, flush persisted pages into database as soon as possible")
    var lazyFlush = false
    @Parameter(names = ["-preferParallel", "--prefer-parallel"], arity = 1,
            description = "Parallel fetch pages whenever applicable")
    var preferParallel = true

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
    var parse = false
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

    // A volatile config is usually session scoped
    var volatileConfig: VolatileConfig? = null
        set(value) { field = initConfig(value) }
        get() = initConfig(field)

    val ignoreQuery get() = shortenKey

    open val modifiedParams: Params get() {
        val rowFormat = "%40s: %s"
        val fields = LoadOptions::class.java.declaredFields
        return fields.filter { it.annotations.any { it is Parameter } && !isDefault(it.name) }
                .onEach { it.isAccessible = true }
                .filter { it.get(this) != null }
                .associate { "-${it.name}" to it.get(this) }
                .let { Params.of(it).withRowFormat(rowFormat) }
    }

    open val modifiedOptions: Map<String, Any> get() {
        val fields = LoadOptions::class.java.declaredFields
        return fields.filter { it.annotations.any { it is Parameter } && !isDefault(it.name) }
                .onEach { it.isAccessible = true }
                .filter { it.get(this) != null }
                .associate { it.name to it.get(this) }
    }

    protected constructor() {
        addObjects(this)
    }

    protected constructor(args: Array<String>) : super(args) {
        addObjects(this)
    }

    protected constructor(args: String) : super(args) {
        addObjects(this)
    }

    open fun createItemOption(): LoadOptions {
        val itemOptions = clone()

        itemOptions.expires = itemExpires
        itemOptions.scrollCount = itemScrollCount
        itemOptions.scriptTimeout = itemScriptTimeout
        itemOptions.scrollInterval = itemScrollInterval
        itemOptions.pageLoadTimeout = itemPageLoadTimeout
        itemOptions.requireNotBlank = itemRequireNotBlank
        itemOptions.requireSize = itemRequireSize
        itemOptions.requireImages = itemRequireImages
        itemOptions.requireAnchors = itemRequireAnchors

        itemOptions.browser = itemBrowser
        if (itemOptions.browser == BrowserType.NATIVE) {
            itemOptions.fetchMode = FetchMode.NATIVE
        }

        itemOptions.volatileConfig = volatileConfig

        return itemOptions
    }

    open fun isDefault(option: String): Boolean {
        val value = LoadOptions::class.java.declaredFields.find { it.name == option }
                ?.also { it.isAccessible = true }?.get(this)?:return false
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
        return modifiedParams.distinct().sorted().withCmdLineStyle(true).withKVDelimiter(" ")
                .formatAsLine().replace("\\s+".toRegex(), " ")
    }

    override fun equals(other: Any?): Boolean {
        return other is LoadOptions && other.toString() == toString()
    }

    // TODO: can not rely on any member filed because static filed defaultParams uses hashCode but none of the fileds is initialized
    override fun hashCode(): Int {
        return super.hashCode()
    }

    /**
     * Create a new LoadOptions
     * */
    open fun clone(): LoadOptions {
        return parse(toString(), volatileConfig)
    }

    /**
     * Merge this LoadOptions and other LoadOptions, return a new LoadOptions
     * */
    open fun mergeModified(other: LoadOptions): LoadOptions {
        val modified = other.modifiedOptions

        LoadOptions::class.java.declaredFields.forEach {
            if (it.name in modified.keys) {
                it.isAccessible = true
                it.set(this, modified[it.name])
            }
        }

        // the fields of sub classes
        this.javaClass.declaredFields.forEach {
            if (it.name in modified.keys) {
                it.isAccessible = true
                it.set(this, modified[it.name])
            }
        }

        return this
    }

    private fun initConfig(vc: VolatileConfig?): VolatileConfig? {
        vc?.setInt(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, scrollCount)
        vc?.setDuration(CapabilityTypes.FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        vc?.setDuration(CapabilityTypes.FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        vc?.setDuration(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
        return vc
    }

    companion object {

        val default = LoadOptions()
        val defaultParams = LoadOptions::class.java.declaredFields.associate { it.name to it.get(default) }
        val defaultArgsMap = default.toArgsMap()
        val optionNames = LoadOptions::class.java.declaredFields
                .flatMap { it.annotations.toList() }
                .filterIsInstance<Parameter>()
                .flatMap { it.names.toList() }

        val helpList: List<List<String>> get() =
                LoadOptions::class.java.declaredFields
                        .mapNotNull { (it.annotations.firstOrNull { it is Parameter } as? Parameter)?.to(it) }
                        .map {
                            listOf(it.first.names.joinToString { it },
                                    it.second.type.typeName.substringAfterLast("."),
                                    defaultParams[it.second.name].toString(),
                                    it.first.description
                            )
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
            val options = LoadOptions(args)
            options.parse()
            options.volatileConfig = volatileConfig
            return options
        }

        @JvmOverloads
        fun mergeModified(o1: LoadOptions, o2: LoadOptions, volatileConfig: VolatileConfig? = null): LoadOptions {
            val options = LoadOptions()
            options.volatileConfig = volatileConfig
            return options.mergeModified(o1).mergeModified(o2)
        }
    }
}
