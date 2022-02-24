package ai.platon.pulsar.common.options

import ai.platon.pulsar.browser.driver.EmulateSettings
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.JsEventHandler
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.FetchMode
import com.beust.jcommander.Parameter
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.kotlinProperty

object LoadOptionDefaults {
    var taskTime = Instant.now().truncatedTo(ChronoUnit.MINUTES)
    /**
     * The default expire time, some time we may need expire all pages by default, for example, in test mode
     * */
    var expires = ChronoUnit.DECADES.duration
    /**
     * The default time to expire
     * */
    var expireAt = DateTimes.doomsday
    var lazyFlush = true
    var parse = false
    var storeContent = true
    /**
     * Retry or not if a page is gone
     * */
    var ignoreFailure = false
    /**
     * The are several cases to enable jit retry
     * For example, in test environment
     * */
    var nJitRetry = -1
    /**
     * The default browser
     * */
    var browser = BrowserType.CHROME
    /**
     * Set to be true if we are doing unit test or other test
     * We will talk more, log more and trace more in test mode
     * */
    var test = 0
}

/**
 * Created by vincent on 19-4-24.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * NOTICE: every option with name `optionName` has to take a Parameter name [-optionName]
 */
open class LoadOptions(
    argv: Array<String>,
    val conf: VolatileConfig
): CommonOptions(argv) {

    @ApiPublic
    @Parameter(names = ["-l", "-label", "--label"], description = "The label of this load task")
    var label = ""

    @ApiPublic
    @Parameter(names = ["-taskId", "--task-id"], description = "The task id. A task can contain multiple loadings")
    var taskId = ""

    /**
     * The task time accepts date time format as the following:
     * 1. ISO_INSTANT: yyyy-MM-ddThh:MM:ssZ
     * 2. yyyy-MM-dd[ hh[:MM[:ss]]]
     * */
    @ApiPublic
    @Parameter(names = ["-taskTime", "--task-time"], converter = InstantConverter::class,
            description = "The task time, we usually use a task time to indicate a batch of a task")
    var taskTime = LoadOptionDefaults.taskTime

    @ApiPublic
    @Parameter(names = ["-deadTime", "--dead-time"], converter = InstantConverter::class,
        description = "The dead time, if now > deadTime, the task should be discarded as soon as possible")
    var deadTime = DateTimes.doomsday

    @ApiPublic
    @Parameter(names = ["-authToken", "--auth-token"], description = "The auth token for this load task")
    var authToken = ""

    @ApiPublic
    @Parameter(names = ["-readonly"], description = "The task does not change the status of the web page")
    var readonly = false

    /**
     * Web page expiry time
     * The term "expires" usually be used for a expiry time, for example, http-equiv, or in cookie specification,
     * guess it means "expires at"
     *
     * The expires field supports both ISO-8601 standard and hadoop time duration format
     * ISO-8601 standard : PnDTnHnMn.nS
     * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
     * */
    @ApiPublic
    @Parameter(names = ["-i", "-expires", "--expires"], converter = DurationConverter::class,
            description = "If a page is expired, it should be fetched from the internet again")
    var expires = LoadOptionDefaults.expires

    /**
     * The page is expired if the current time > expireAt
     * */
    @ApiPublic
    @Parameter(names = ["-expireAt", "--expire-at"], converter = InstantConverter::class,
            description = "If a page is expired, it should be fetched from the internet again")
    var expireAt = LoadOptionDefaults.expireAt

    /**
     * The page is expired if the current time > expireAt
     * */
    @ApiPublic
    @Parameter(names = ["-fi", "-fetchInterval", "--fetch-interval"], converter = DurationConverter::class,
        description = "If a page is expired, it should be fetched from the internet again")
    var fetchInterval = ChronoUnit.DECADES.duration

    /** Arrange links */
    @ApiPublic
    @Parameter(names = ["-ol", "-outLink", "-outLinkSelector", "--out-link-selector", "-outlink", "-outlinkSelector", "--outlink-selector"],
            description = "The CSS selector by which the anchors in the portal page are selected to load and analyze, " +
                    "Out pages will be detected automatically if the selector is empty")
    var outLinkSelector = ""

    @ApiPublic
    @Parameter(names = ["-olp", "-outLinkPattern", "--out-link-pattern"], description = "The pattern of the out links")
    var outLinkPattern = ".+"

    @ApiPublic
    @Parameter(names = ["-np", "-nextPage", "-nextPageSelector", "--next-page-selector"],
            description = "[TODO] The css selector of next page anchor")
    var nextPageSelector = ""

    @ApiPublic
    @Parameter(names = ["-ifr", "-iframe", "--iframe"], description = "The i-th iframe")
    var iframe = 0

    @ApiPublic
    @Parameter(names = ["-tl", "-topLinks", "--top-links"], description = "Top N links")
    var topLinks = 20

    @ApiPublic
    @Parameter(names = ["-tng", "-topNAnchorGroups", "--top-anchor-groups"], description = "Try the top N anchor groups")
    var topNAnchorGroups = 3

    @ApiPublic
    @Parameter(names = ["-wnb", "-waitNonBlank"],
            description = "[TODO] Wait for ajax content until the element is filled by a non-blank text")
    var waitNonBlank: String = ""

    @ApiPublic
    @Parameter(names = ["-rnb", "-requireNotBlank"], description = "[TODO] Keep the pages only if the required text is not blank")
    var requireNotBlank: String = ""

    @ApiPublic
    @Parameter(names = ["-rs", "-requireSize", "--require-size"], description = "Fetch pages smaller than requireSize in bytes")
    var requireSize = 0

    @ApiPublic
    @Parameter(names = ["-ri", "-requireImages", "--require-images"], description = "Fetch pages who's images less than requireImages")
    var requireImages = 0

    @ApiPublic
    @Parameter(names = ["-ra", "-requireAnchors", "--require-anchors"], description = "Fetch pages who's anchors less than requireAnchors")
    var requireAnchors = 0

    @Parameter(names = ["-fm", "-fetchMode", "--fetch-mode"], converter = FetchModeConverter::class,
            description = "The fetch mode, native, crowd sourcing and selenium are supported, selenium is the default")
    var fetchMode = FetchMode.BROWSER

    @Parameter(names = ["-b", "-browser", "--browser"], converter = BrowserTypeConverter::class,
            description = "The browser to use, google chrome is the default")
    var browser = LoadOptionDefaults.browser

    @Parameter(names = ["-sc", "-scrollCount", "--scroll-count"],
            description = "The count to scroll down after a page is opened by a browser")
    var scrollCount = EmulateSettings.DEFAULT.scrollCount

    @Parameter(names = ["-si", "-scrollInterval", "--scroll-interval"], converter = DurationConverter::class,
            description = "The interval to scroll down after a page is opened by a browser")
    var scrollInterval = EmulateSettings.DEFAULT.scrollInterval

    @Parameter(names = ["-stt", "-scriptTimeout", "--script-timeout"], converter = DurationConverter::class,
            description = "The maximum time to perform javascript injected into selenium")
    var scriptTimeout = EmulateSettings.DEFAULT.scriptTimeout

    @Parameter(names = ["-plt", "-pageLoadTimeout", "--page-load-timeout"], converter = DurationConverter::class,
            description = "The maximum time to wait for a page to finish from the first http request start")
    var pageLoadTimeout = EmulateSettings.DEFAULT.pageLoadTimeout

    // itemXXX should be available for all index-item pattern pages
    @Parameter(names = ["-ib", "-itemBrowser", "--item-browser"], converter = BrowserTypeConverter::class,
            description = "The browser used to visit the item pages, CHROME and NATIVE are supported")
    var itemBrowser = BrowserType.CHROME

    @ApiPublic
    @Parameter(names = ["-ii", "-itemExpires", "--item-expires"], converter = DurationConverter::class,
            description = "The same as expires, but only works for item pages in harvest tasks")
    var itemExpires = ChronoUnit.DECADES.duration

    /** Web page expire time */
    @ApiPublic
    @Parameter(names = ["-itemExpireAt", "--item-expire-at"], converter = InstantConverter::class,
            description = "If a page is expired, it should be fetched from the internet again")
    var itemExpireAt = DateTimes.doomsday

    /** Note: if scroll too many times, the page may fail to calculate the vision information */
    @Parameter(names = ["-isc", "-itemScrollCount", "--item-scroll-count"],
            description = "The same as scrollCount, but only works for item pages in harvest tasks")
    var itemScrollCount = scrollCount

    @Parameter(names = ["-isi", "-itemScrollInterval", "--item-scroll-interval"], converter = DurationConverter::class,
            description = "The same as scrollInterval, but only works for item pages in some batch tasks")
    var itemScrollInterval = scrollInterval

    @Parameter(names = ["-ist", "-itemScriptTimeout", "--item-script-timeout"], converter = DurationConverter::class,
            description = "The same as scriptTimeout, but only works for item pages in some batch tasks")
    var itemScriptTimeout = scriptTimeout

    @Parameter(names = ["-iplt", "-itemPageLoadTimeout", "--item-page-load-timeout"], converter = DurationConverter::class,
            description = "The same as pageLoadTimeout, but only works for item pages in some batch tasks")
    var itemPageLoadTimeout = pageLoadTimeout

    @ApiPublic
    @Parameter(names = ["-irnb", "-itemRequireNotBlank", "--item-require-not-blank"],
            description = "Keep the item pages only if the required text is not blank")
    var itemRequireNotBlank = ""

    @ApiPublic
    @Parameter(names = ["-irs", "-itemRequireSize", "--item-require-size"],
            description = "Fetch item pages smaller than requireSize")
    var itemRequireSize = 0

    @ApiPublic
    @Parameter(names = ["-iri", "-itemRequireImages", "--item-require-images"],
            description = "Fetch item pages who's images less than requireImages")
    var itemRequireImages = 0

    @ApiPublic
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

    @ApiPublic
    @Parameter(names = ["-refresh", "--refresh"],
        description = "Refresh the fetch state of page, clear fetch retry counter" +
                " -refresh = -ignoreFailure -i 0s and set page.fetchRetries = 0")
    var refresh = false

    /**
     * Force retry fetching the page if it's failed last time, or it's marked as gone
     * This option is deprecated and be replaced by ignoreFailure which is more descriptive
     * */
    @Deprecated("Replaced by ignoreFailure, will be removed in further versions")
    @ApiPublic
    @Parameter(names = ["-retry", "--retry", "-retryFailed", "--retry-failed"],
            description = "Retry fetching the page even if it's failed last time")
    var retryFailed = LoadOptionDefaults.ignoreFailure

    /**
     * Force retry fetching the page if it's failed last time, or it's marked as gone
     * */
    @ApiPublic
    @Parameter(names = ["-ignF", "-ignoreFailure", "--ignore-failure"],
            description = "Retry fetching the page even if it's failed last time")
    var ignoreFailure = LoadOptionDefaults.ignoreFailure

    @Parameter(names = ["-njr", "-nJitRetry", "--n-jit-retry"],
            description = "Retry at most n times if RETRY(1601) code return when fetching a page")
    var nJitRetry = LoadOptionDefaults.nJitRetry

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

    @Deprecated("Use x-sql instead")
    @Parameter(names = ["-q", "-query", "--query"], description = "Extract query to extract data from")
    var query: String? = null

    @Parameter(names = ["-m", "-withModel", "--with-model"], description = "Also load page model when loading a page")
    var withModel = false

    @Parameter(names = ["-lk", "-withLinks", "--with-links"], description = "Contains links when loading page model")
    var withLinks = false

    @Parameter(names = ["-tt", "-withText", "--with-text"], description = "Contains text when loading page model")
    var withText = false

    @Parameter(
        names = ["-netCond", "-netCondition", "--net-condition"],
        converter = ConditionConverter::class,
        description = "The network condition level"
    )
    var netCondition = Condition.GOOD

    @Parameter(names = ["-test", "--test"], description = "The test level, 0 to disable, we will talk more in test mode")
    var test = LoadOptionDefaults.test

    @Parameter(names = ["-v", "-version", "--version"], description = "The load option version")
    var version = "20210321"

    /**
     * If shortenKey is set, also ignore url query when fetch pages
     * */
    val ignoreQuery get() = shortenKey

    // JCommand do not remove surrounding quotes, like jcommander.parse("-outlink \"ul li a[href~=item]\"")
    val correctedOutLinkSelector get() = outLinkSelector.trim('"')

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

//    private val jsEventHandlers = mutableListOf<JsEventHandler>()

    protected constructor(args: String, conf: VolatileConfig) : this(split(args), conf)

    /**
     * Parse with parameter overwriting fix
     * */
    override fun parse(): Boolean {
        val b = super.parse()
        if (b) {
            // fix zero-arity boolean parameter overwriting
            optionFields.asSequence()
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

        return itemOptions
    }

    /**
     * Check if the page has been expired.
     * A page is expired if
     * 1. the last fetch time is before [expireAt] and now is after [expireAt]
     * 2. (the last fetch time + [expires]) is passed
     * */
    fun isExpired(prevFetchTime: Instant): Boolean {
        val now = Instant.now()
        return when  {
            refresh -> true
            expireAt in prevFetchTime..now -> true
            now >= prevFetchTime + expires -> true
            else -> false
        }
    }

    fun isDead(): Boolean {
        return deadTime < Instant.now()
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
        val emulateSettings = when (netCondition) {
            Condition.WORSE -> EmulateSettings.worseNetSettings
            Condition.WORST -> EmulateSettings.worstNetSettings
            else -> EmulateSettings.goodNetSettings
        }.copy()

        if (!isDefault("scrollCount")) emulateSettings.scrollCount = scrollCount
        if (!isDefault("scrollInterval")) emulateSettings.scrollInterval = scrollInterval
        if (!isDefault("scriptTimeout")) emulateSettings.scriptTimeout = scriptTimeout
        if (!isDefault("pageLoadTimeout")) emulateSettings.pageLoadTimeout = pageLoadTimeout

        emulateSettings.apply(conf)

        setEnum(CapabilityTypes.BROWSER_TYPE, browser)
        setBoolean(CapabilityTypes.BROWSER_INCOGNITO, incognito)
    }

    open fun isDefault(option: String): Boolean {
        val value = optionFieldsMap[option]?.also { it.isAccessible = true }?.get(this) ?: return false
        return value == defaultParams[option]
    }

    override fun getParams(): Params {
        val rowFormat = "%40s: %s"
        return optionFields.filter { it.annotations.any { it is Parameter } }
                .onEach { it.isAccessible = true }
                .associate { "-${it.name}" to it.get(this) }
                .filter { it.value != null }
                .let { Params.of(it).withRowFormat(rowFormat) }
    }

    fun addEventHandler(eventHandler: JsEventHandler?) {
        if (eventHandler != null) {
            // jsEventHandlers.add(eventHandler)
            conf.putBean(eventHandler)
        }
    }

    fun removeEventHandler(eventHandler: JsEventHandler?) {
        if (eventHandler != null) {
            // jsEventHandlers.remove(eventHandler)
            conf.removeBean(eventHandler)
        }
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
    open fun clone() = parse(toString(), conf)

    companion object {
        val default = LoadOptions("", VolatileConfig())
        val optionFields = LoadOptions::class.java.declaredFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .filter { it.annotations.filterIsInstance<Parameter>().isNotEmpty() }
            .onEach {
                val name = it.name
                val count = it.annotations.filterIsInstance<Parameter>().count { it.names.contains("-$name") }
                require(count > 0) { "Missing -$name option for field <$name>" }
            }
        val optionFieldsMap = optionFields.associateBy { it.name }
        val defaultParams = optionFields.associate { it.name to it.get(default) }
        val defaultArgsMap = default.toArgsMap()
        val arity0BooleanParams = optionFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .filter { it.get(default) is Boolean }
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .filter { it.arity < 1 }
            .flatMap { it.names.toList() }
            .toList()
        val arity1BooleanParams = optionFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .filter { it.get(default) is Boolean }
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .filter { it.arity == 1 }
            .flatMap { it.names.toList() }
            .toList()
        val optionNames = optionFields
            .asSequence()
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .flatMap { it.names.toList() }
            .toList()
        val apiPublicOptionNames = optionFields
            .asSequence()
            .filter { it.kotlinProperty?.hasAnnotation<ApiPublic>() == true }
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .flatMap { it.names.toList() }
            .toList()

        val helpList: List<List<String>> get() =
            optionFields
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
            optionFields.forEach {
                val found = it.annotations.filterIsInstance<Parameter>().any { annotationName in it.names }
                if (found) {
                    it.isAccessible = true
                    it.set(options, value)
                }
            }
        }

        fun getOptionNames(fieldName: String): List<String> {
            return optionFields
                .asSequence()
                .filter { it.name == fieldName }
                .flatMap { it.annotations.toList() }
                .filterIsInstance<Parameter>()
                .flatMap { it.names.toList() }
                .toList()
        }

        fun create(conf: VolatileConfig) = LoadOptions(arrayOf(), conf).apply { parse() }

        fun parse(args: String, conf: VolatileConfig) = LoadOptions(args.trim(), conf).apply { parse() }

        /**
         * Create a new LoadOptions with o1 and o2's items, o2 overrides o1
         * */
        fun merge(o1: LoadOptions, o2: LoadOptions) = parse("$o1 $o2", o1.conf)

        fun merge(o1: LoadOptions, args: String?) = parse("$o1 $args", o1.conf)

        fun merge(args: String?, args2: String?, conf: VolatileConfig) = parse("$args $args2", conf)

        fun eraseOptions(args: String, vararg fieldNames: String): String {
            // do not forget the blanks
            var normalizedArgs = " $args "

            val optionNames = fieldNames.flatMap { getOptionNames(it) }.map { " $it " }
            optionNames.forEach {
                normalizedArgs = normalizedArgs.replace(it, " -erased ")
            }

            return normalizedArgs.trim()
        }
    }
}
