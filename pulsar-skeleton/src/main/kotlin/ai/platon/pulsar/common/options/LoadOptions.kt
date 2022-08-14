package ai.platon.pulsar.common.options

import ai.platon.pulsar.browser.common.EmulateSettings
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.DefaultPulsarEventHandler
import ai.platon.pulsar.crawl.PulsarEventHandler
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.dom.select.appendSelectorIfMissing
import ai.platon.pulsar.persist.metadata.FetchMode
import com.beust.jcommander.Parameter
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.kotlinProperty

/**
 * The default load options, be careful to change the default behaviour.
 * */
object LoadOptionDefaults {
    /**
     * The default task time.
     * */
    var taskTime = Instant.now().truncatedTo(ChronoUnit.MINUTES)
    /**
     * The default expiry time, some time we may need expire all pages by default, for example, in test mode
     * */
    var expires = ChronoUnit.DECADES.duration
    /**
     * The default time to expire
     * */
    var expireAt = DateTimes.doomsday
    /**
     * Lazy flush.
     * */
    var lazyFlush = true
    /**
     * Trigger the parse phrase or not.
     *
     * Do not parse by default, since there are may ways to trigger a webpage parsing:
     * 1. use session.parse()
     * 2. add a -parse option
     * 3. use a [ai.platon.pulsar.crawl.common.url.ParsableHyperlink]
     * */
    var parse = false
    /**
     * Store webpage content or not.
     *
     * Store webpage content by default.
     * If we are running a public cloud, this option might be changed to false.
     * */
    var storeContent = true
    /**
     * If true, still fetch the page even if it is gone.
     * */
    var ignoreFailure = false
    /**
     * There are several cases to enable jit retry.
     * For example, in a test environment.
     * */
    var nJitRetry = -1
    /**
     * The default browser is chrome with pulsar implemented web driver.
     * */
    var browser = BrowserType.PULSAR_CHROME
    /**
     * Set to be > 0 if we are doing unit test or other test.
     * We will talk more, log more and trace more in test mode.
     * */
    var test = 0
}

/**
 * LoadOptions can represent the controling arguments which effects how we load a webpage:
 *
 * ```kotlin
 * // fetch only when after 1d since last fetch
 * session.load('https://www.jd.com', '-expires 1d')
 * // fetch now ignoring errors
 * session.load('https://www.jd.com', '-refresh')
 * // do not fetch after dead time
 * session.load('https://www.jd.com', '-deadTime 2022-04-15T18:36:54.941Z')
 * // activate the parse phrase
 * session.load('https://www.jd.com', '-parse')
 * // write the page content into storage
 * session.load('https://www.jd.com', '-storeContent')
 * ```
 *
 * NOTICE: every option with name `optionName` has to take a [Parameter] name [-optionName].
 */
open class LoadOptions(
    argv: Array<String>,
    val conf: VolatileConfig,
    var eventHandler: PulsarEventHandler? = null,
    var itemEventHandler: PulsarEventHandler? = null
): CommonOptions(argv) {

    /**
     * The label of this load task.
     * */
    @ApiPublic
    @Parameter(names = ["-l", "-label", "--label"], description = "The label of this load task")
    var label = ""

    /**
     * The task id. A task can contain multiple loads.
     * */
    @ApiPublic
    @Parameter(names = ["-taskId", "--task-id"],
        description = "The task id. A task can contain multiple loads")
    var taskId = ""

    /**
     * The task time, we usually use a task time to indicate the name of a batch task.
     * */
    @ApiPublic
    @Parameter(names = ["-taskTime", "--task-time"], converter = InstantConverter::class,
            description = "The task time, we usually use a task time to indicate the name of a batch task")
    var taskTime = LoadOptionDefaults.taskTime

    /**
     * The dead time, if now > deadTime, the task should be discarded as soon as possible.
     * */
    @ApiPublic
    @Parameter(names = ["-deadTime", "--dead-time"], converter = InstantConverter::class,
        description = "The dead time, if now > deadTime, the task should be discarded as soon as possible")
    var deadTime = DateTimes.doomsday

    /**
     * The auth token for this task.
     * */
    @ApiPublic
    @Parameter(names = ["-authToken", "--auth-token"], description = "The auth token for this task")
    var authToken = ""

    /**
     * If true, get a copy of the webpage and do not modify it.
     * */
    @ApiPublic
    @Parameter(names = ["-readonly"], description = "If true, get a copy of the webpage and do not modify it")
    var readonly = false

    /**
     * If true, fetch the page content as a resource without browser rendering.
     * */
    @ApiPublic
    @Parameter(names = ["-resource", "-isResource"],
        description = "If true, fetch the page content as a resource without browser rendering")
    var isResource = false

    /**
     * The expiry time. If a page is expired, it should be fetched from the web.
     *
     * The term "expires" usually be used for an expiry time, for example, http-equiv, or in cookie specification,
     * guess it means "expires at".
     *
     * The expires field supports both ISO-8601 standard and hadoop time duration format:
     * ISO-8601 standard : PnDTnHnMn.nS
     * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
     * */
    @ApiPublic
    @Parameter(names = ["-i", "-expires", "--expires"], converter = DurationConverter::class,
            description = "The expiry time. If a page is expired, it should be fetched from the web")
    var expires = LoadOptionDefaults.expires

    /**
     * The time point to expire. If a page is expired, it should be fetched from the web.
     * */
    @ApiPublic
    @Parameter(names = ["-expireAt", "--expire-at"], converter = InstantConverter::class,
            description = "The time point to expire. If a page is expired, it should be fetched from the web")
    var expireAt = LoadOptionDefaults.expireAt

    /**
     * The fetch interval, used for periodically tasks.
     * */
    @ApiPublic
    @Parameter(names = ["-fi", "-fetchInterval", "--fetch-interval"], converter = DurationConverter::class,
        description = "The fetch interval, used for periodically tasks")
    var fetchInterval = ChronoUnit.DECADES.duration

    /**
     * The CSS selector to select out links in the portal page.
     * */
    @ApiPublic
    @Parameter(names = ["-ol", "-outLink", "-outLinkSelector", "--out-link-selector", "-outlink", "-outlinkSelector", "--outlink-selector"],
            description = "The CSS selector to select out links in the portal page")
    var outLinkSelector = ""

    /**
     * The pattern to select out links in the portal page.
     * */
    @ApiPublic
    @Parameter(names = ["-olp", "-outLinkPattern", "--out-link-pattern"],
        description = "The pattern to select out links in the portal page")
    var outLinkPattern = ".+"

    /**
     * The selector of the element to click for out links, if it's blank, no element should be clicked.
     * not implemented yet
     * */
    @ApiPublic
    @Parameter(
        names = ["-click", "-clickTarget", "--click-target"],
        description = "The selector of the element to click for out links, if it's blank, no element should be clicked"
    )
    var clickTarget = ""

    /**
     * The css selector of next page anchor.
     * not implemented yet
     * */
    @ApiPublic
    @Parameter(names = ["-np", "-nextPage", "-nextPageSelector", "--next-page-selector"],
            description = "The css selector of next page anchor")
    var nextPageSelector = ""

    /**
     * The iframe id to switch to.
     * */
    @ApiPublic
    @Parameter(names = ["-ifr", "-iframe", "--iframe"], description = "The iframe id to switch to")
    var iframe = 0

    /**
     * Specify how many links to select if we deal with out-pages.
     * */
    @ApiPublic
    @Parameter(names = ["-tl", "-topLinks", "--top-links"],
        description = "Specify how many links to select if we deal with out-pages.")
    var topLinks = 20

    /**
     * Try the top N anchor groups.
     * */
    @ApiPublic
    @Parameter(names = ["-tng", "-topNAnchorGroups", "--top-anchor-groups"], description = "Try the top N anchor groups")
    var topNAnchorGroups = 3

    /**
     * Wait for ajax content until the element is filled by a non-blank text.
     * */
    @ApiPublic
    @Parameter(names = ["-wnb", "-waitNonBlank"],
            description = "[TODO] Wait for ajax content until the element is filled by a non-blank text")
    var waitNonBlank: String = ""

    /**
     * Keep the pages only if the required text is not blank.
     * */
    @ApiPublic
    @Parameter(names = ["-rnb", "-requireNotBlank"], description = "[TODO] Keep the pages only if the required text is not blank")
    var requireNotBlank: String = ""

    /**
     * Fetch pages smaller than requireSize in bytes.
     * */
    @ApiPublic
    @Parameter(names = ["-rs", "-requireSize", "--require-size"], description = "Fetch pages smaller than requireSize in bytes")
    var requireSize = 0

    /**
     * Re-fetch a page who's images less than requireImages.
     * */
    @ApiPublic
    @Parameter(names = ["-ri", "-requireImages", "--require-images"],
        description = "Re-fetch a page who's images less than requireImages")
    var requireImages = 0

    /**
     * Re-fetch a page who's anchors less than requireAnchors.
     * */
    @ApiPublic
    @Parameter(names = ["-ra", "-requireAnchors", "--require-anchors"],
        description = "Re-fetch a page who's anchors less than requireAnchors")
    var requireAnchors = 0

    /**
     * The fetch mode.
     * */
    @Parameter(names = ["-fm", "-fetchMode", "--fetch-mode"], converter = FetchModeConverter::class,
            description = "The fetch mode")
    var fetchMode = FetchMode.BROWSER

    /**
     * Specify which browser to use, google chrome is the default.
     * TODO: session scope browser choice is not support by now
     * */
    @Parameter(names = ["-b", "-browser", "--browser"], converter = BrowserTypeConverter::class,
            description = "Specify which browser to use, google chrome is the default")
    var browser = LoadOptionDefaults.browser

    /**
     * The count to scroll down after a page being opened in a browser.
     * */
    @Parameter(names = ["-sc", "-scrollCount", "--scroll-count"],
            description = "The count to scroll down after a page being opened in a browser")
    var scrollCount = EmulateSettings.DEFAULT.scrollCount

    /**
     * The interval to scroll down after a page being opened in a browser.
     * */
    @Parameter(names = ["-si", "-scrollInterval", "--scroll-interval"], converter = DurationConverter::class,
            description = "The interval to scroll down after a page being opened in a browser")
    var scrollInterval = EmulateSettings.DEFAULT.scrollInterval

    /**
     * The maximum time to perform javascript injected into the browser.
     * */
    @Parameter(names = ["-stt", "-scriptTimeout", "--script-timeout"], converter = DurationConverter::class,
            description = "The maximum time to perform javascript injected into the browser")
    var scriptTimeout = EmulateSettings.DEFAULT.scriptTimeout

    /**
     * The maximum time to wait for a page to finish.
     * */
    @Parameter(names = ["-plt", "-pageLoadTimeout", "--page-load-timeout"], converter = DurationConverter::class,
            description = "The maximum time to wait for a page to finish")
    var pageLoadTimeout = EmulateSettings.DEFAULT.pageLoadTimeout

    /**
     * The browser used to visit the item pages.
     * TODO: session scope browser choice is not support by now
     * */
    @Parameter(names = ["-ib", "-itemBrowser", "--item-browser"], converter = BrowserTypeConverter::class,
            description = "The browser used to visit the item pages")
    var itemBrowser = LoadOptionDefaults.browser

    /**
     * The same as expires, but only works for item pages.
     * */
    @ApiPublic
    @Parameter(names = ["-ii", "-itemExpires", "--item-expires"], converter = DurationConverter::class,
            description = "The same as expires, but only works for item pages")
    var itemExpires = ChronoUnit.DECADES.duration

    /**
     * If an item page is expired, it should be fetched from the web again.
     * */
    @ApiPublic
    @Parameter(names = ["-itemExpireAt", "--item-expire-at"], converter = InstantConverter::class,
            description = "If an item page is expired, it should be fetched from the web again")
    var itemExpireAt = DateTimes.doomsday

    /**
     * The same as scrollCount, but only works for item pages.
     * */
    @Parameter(names = ["-isc", "-itemScrollCount", "--item-scroll-count"],
            description = "The same as scrollCount, but only works for item pages")
    var itemScrollCount = scrollCount

    /**
     * The same as scrollInterval, but only works for item pages.
     * */
    @Parameter(names = ["-isi", "-itemScrollInterval", "--item-scroll-interval"], converter = DurationConverter::class,
            description = "The same as scrollInterval, but only works for item pages")
    var itemScrollInterval = scrollInterval

    /**
     * The same as scriptTimeout, but only works for item pages.
     * */
    @Parameter(names = ["-ist", "-itemScriptTimeout", "--item-script-timeout"], converter = DurationConverter::class,
            description = "The same as scriptTimeout, but only works for item pages")
    var itemScriptTimeout = scriptTimeout

    /**
     * The same as pageLoadTimeout, but only works for item pages.
     * */
    @Parameter(names = ["-iplt", "-itemPageLoadTimeout", "--item-page-load-timeout"], converter = DurationConverter::class,
            description = "The same as pageLoadTimeout, but only works for item pages")
    var itemPageLoadTimeout = pageLoadTimeout

    /**
     * Re-fetch the item pages if the required text is blank.
     * */
    @ApiPublic
    @Parameter(names = ["-irnb", "-itemRequireNotBlank", "--item-require-not-blank"],
            description = "Re-fetch the item pages if the required text is blank")
    var itemRequireNotBlank = ""

    /**
     * Re-fetch item pages smaller than requireSize.
     * */
    @ApiPublic
    @Parameter(names = ["-irs", "-itemRequireSize", "--item-require-size"],
            description = "Re-fetch item pages smaller than requireSize")
    var itemRequireSize = 0

    /**
     * Re-fetch item pages who's images is less than requireImages.
     * */
    @ApiPublic
    @Parameter(names = ["-iri", "-itemRequireImages", "--item-require-images"],
            description = "Re-fetch item pages who's images is less than requireImages")
    var itemRequireImages = 0

    /**
     * Re-fetch item pages who's anchors is less than requireAnchors.
     * */
    @ApiPublic
    @Parameter(names = ["-ira", "-itemRequireAnchors", "--item-require-anchors"],
            description = "Re-fetch item pages who's anchors is less than requireAnchors")
    var itemRequireAnchors = 0

    /**
     * Persist fetched pages as soon as possible.
     * */
    @Parameter(names = ["-persist", "--persist"], arity = 1,
            description = "Persist fetched pages as soon as possible")
    var persist = true

    /**
     * If false, persist the page without the content which is usually very large
     * */
    @Parameter(names = ["-storeContent", "--store-content"], arity = 1,
            description = "If false, persist the page without it's content which is usually very large")
    var storeContent = LoadOptionDefaults.storeContent

    /**
     * Refresh the fetch state of a page, clear the retry counters.
     * If true, the page should be fetched, just like we click the refresh button on a real browser.
     * The option can be explained as follows: -refresh = -ignoreFailure -i 0s and set page.fetchRetries = 0.
     *
     * TODO: consider add an option itemRefresh
     * */
    @ApiPublic
    @Parameter(names = ["-refresh", "--refresh"],
        description = "Refresh the fetch state of a page, clear the retry counters." +
                " If true, the page should be fetched immediately." +
                " The option can be explained as follows:" +
                " -refresh = -ignoreFailure -i 0s and set page.fetchRetries = 0")
    var refresh = false
        set(value) = doRefresh(value)

    /**
     * Force retry fetching the page if it's failed last time, or it's marked as gone.
     * */
    @Deprecated("Replaced by ignoreFailure, will be removed in further versions")
    @ApiPublic
    @Parameter(names = ["-retry", "--retry", "-retryFailed", "--retry-failed"],
            description = "Retry fetching the page even if it's failed last time, or it's marked as gone")
    var retryFailed = LoadOptionDefaults.ignoreFailure

    /**
     * Retry fetching the page even if it's failed last time.
     * */
    @ApiPublic
    @Parameter(names = ["-ignF", "-ignoreFailure", "--ignore-failure"],
            description = "Retry fetching the page even if it's failed last time")
    var ignoreFailure = LoadOptionDefaults.ignoreFailure

    /**
     * Retry to fetch at most n times, if page.fetchRetries > nMaxRetry,
     * the page is marked as gone and do not fetch it again until -refresh is set to clear page.fetchRetries
     * */
    @Parameter(names = ["-nmr", "-nMaxRetry", "--n-max-retry"],
        description = "Retry to fetch at most n times, if page.fetchRetries > nMaxRetry," +
                " the page is marked as gone and do not fetch it again until -refresh is set to clear page.fetchRetries")
    var nMaxRetry = 3

    /**
     * Retry at most n times at fetch phrase immediately if RETRY(1601) code return.
     * */
    @Parameter(names = ["-njr", "-nJitRetry", "--n-jit-retry"],
            description = "Retry at most n times at fetch phrase immediately if RETRY(1601) code return")
    var nJitRetry = LoadOptionDefaults.nJitRetry

    /**
     * If false, pages are flushed into database as soon as possible.
     * */
    @Parameter(names = ["-lazyFlush", "--lazy-flush"],
            description = "If false, pages are flushed into database as soon as possible")
    var lazyFlush = LoadOptionDefaults.lazyFlush

    /**
     * Parallel fetch pages whenever applicable.
     * */
    @Deprecated("Not used since there is no non-parallel fetching")
    @Parameter(names = ["-preferParallel", "--prefer-parallel"], arity = 1,
            description = "Parallel fetch pages whenever applicable")
    var preferParallel = true

    /**
     * Run browser in incognito mode.
     * Not used since the browser is always running in temporary contexts
     * */
    @Deprecated("Not used since the browser is always running in temporary contexts")
    @Parameter(names = ["-ic", "-incognito", "--incognito"], description = "Run browser in incognito mode")
    var incognito = false

    /**
     * Do not redirect.
     * Ignored in browser mode since the browser handles the redirection itself.
     * */
    @Parameter(names = ["-noRedirect", "--no-redirect"], description = "Do not redirect")
    var noRedirect = false

    /**
     * If false, return the original page record but the redirect target's content,
     * otherwise, return the page record of the redirected target.
     * If we use a browser, redirections are handled by the browser so the flag is ignored.
     * */
    @Parameter(names = ["-hardRedirect", "--hard-redirect"],
            description = "If false, return the original page record but the redirect target's content," +
                    " otherwise, return the page record of the redirected target." +
                    " If we use a browser, redirections are handled by the browser so the flag is ignored.")
    var hardRedirect = false

    /**
     * If true, run the parse phrase.
     * */
    @Parameter(names = ["-ps", "-parse", "--parse"], description = "If true, parse the page after fetch")
    var parse = LoadOptionDefaults.parse

    /**
     * Re-parse all links if the page is parsed.
     * */
    @Parameter(names = ["-rpl", "-reparseLinks", "--reparse-links"], description = "Re-parse all links if the page is parsed")
    var reparseLinks = false

    /**
     * If true, remove the query parameters in the url.
     * */
    @Parameter(names = ["-ignoreUrlQuery", "--ignore-url-query"], description = "Remove the query parameters in the url")
    var ignoreUrlQuery = false

    /**
     * No normalizer is applied to parse links.
     * */
    @Parameter(names = ["-noNorm", "--no-link-normalizer"], description = "No normalizer is applied to parse links")
    var noNorm = false

    /**
     * No filter is applied to parse links.
     * */
    @Parameter(names = ["-noFilter", "--no-link-filter"], description = "No filter is applied to parse links")
    var noFilter = false

    /**
     * Specify the network condition.
     * */
    @Parameter(
        names = ["-netCond", "-netCondition", "--net-condition"],
        converter = ConditionConverter::class,
        description = "Indicates the network condition"
    )
    var netCondition = Condition.GOOD

    /**
     * The test level, 0 to disable, we will talk more in test mode.
     * */
    @Parameter(names = ["-test", "--test"], description = "The test level, 0 to disable, we will talk more in test mode")
    var test = LoadOptionDefaults.test

    /**
     * The load option version.
     * */
    @Parameter(names = ["-v", "-version", "--version"], description = "The load option version")
    var version = "20210321"

    /**
     * Get the corrected [outLinkSelector] or null. See [outLinkSelector] for more information.
     * */
    val outLinkSelectorOrNull
        get() = outLinkSelector.takeIf { it.isNotBlank() }

    /**
     * The page referrer.
     * */
    var referrer: String? = null

    /**
     * Find out the modified fields and return a [Params].
     * */
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

    /**
     * Find out the modified fields and return a map.
     * */
    open val modifiedOptions: Map<String, Any>
        get() {
            val fields = LoadOptions::class.java.declaredFields
            return fields.filter { it.annotations.any { it is Parameter } && !isDefault(it.name) }
                    .onEach { it.isAccessible = true }
                    .filter { it.get(this) != null }
                    .associate { it.name to it.get(this) }
        }

    /**
     * The constructor.
     * */
    protected constructor(args: String, conf: VolatileConfig) : this(split(args), conf)

    /**
     * The constructor.
     * */
    protected constructor(args: String, other: LoadOptions) :
            this(split(args), other.conf, other.eventHandler, other.itemEventHandler)

    /**
     * Ensure the EventHandler is created.
     * */
    fun ensureEventHandler(): PulsarEventHandler {
        val eh = eventHandler ?: DefaultPulsarEventHandler()
        eventHandler = eh
        return eh
    }

    fun ensureItemEventHandler(): PulsarEventHandler {
        val eh = eventHandler ?: DefaultPulsarEventHandler()
        itemEventHandler = eh
        return eh
    }

    /**
     * Parse the arguments into [LoadOptions] with JCommander and with bug fixes.
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
            outLinkSelector = correctOutLinkSelector() ?: ""
        }
        return b
    }

    /**
     * Create options for item pages.
     * */
    open fun createItemOptions(): LoadOptions {
        val itemOptions = clone()
        itemOptions.itemOptions2MajorOptions()

        if (itemOptions.browser == BrowserType.NATIVE) {
            itemOptions.fetchMode = FetchMode.NATIVE
        }
        itemOptions.eventHandler = itemEventHandler

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

    /**
     * If the page is dead, do not fetch it from the web.
     * */
    fun isDead(): Boolean {
        return deadTime < Instant.now()
    }

    /**
     * Convert the item options to major options. The system do not use item options directly,
     * we have to do the convert before we process item pages.
     * */
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

        eventHandler = itemEventHandler
    }

    /**
     * Write some option values to [conf].
     *
     * [LoadOptions] is not globally visible, we have to pass some values to modules who can not see it
     * through a [VolatileConfig] object.
     * */
    fun overrideConfiguration() = overrideConfiguration(this.conf)

    /**
     * Write some option values to [conf].
     *
     * [LoadOptions] is not globally visible, we have to pass some values to modules who can not see it
     * through a [VolatileConfig] object.
     * */
    fun overrideConfiguration(conf: VolatileConfig?): VolatileConfig? = conf?.apply {
        val emulateSettings = when (netCondition) {
            Condition.WORSE -> EmulateSettings.worseNetSettings
            Condition.WORST -> EmulateSettings.worstNetSettings
            else -> EmulateSettings.goodNetSettings
        }.copy()

        if (!isDefault("scrollCount")) emulateSettings.scrollCount = scrollCount
        if (!isDefault("scrollInterval")) emulateSettings.scrollInterval = scrollInterval
        if (!isDefault("scriptTimeout")) emulateSettings.scriptTimeout = scriptTimeout
        if (!isDefault("pageLoadTimeout")) emulateSettings.pageLoadTimeout = pageLoadTimeout

        emulateSettings.overrideConfiguration(conf)

        eventHandler?.let { putBean(it) }
        setEnum(CapabilityTypes.BROWSER_TYPE, browser)
        // not used since the browser is always running in temporary contexts
        setBoolean(CapabilityTypes.BROWSER_INCOGNITO, incognito)
    }

    /**
     * Check if the option value is the default.
     * */
    open fun isDefault(option: String): Boolean {
        val value = optionFieldsMap[option]?.also { it.isAccessible = true }?.get(this) ?: return false
        return value == defaultParams[option]
    }

    /**
     * Convert the [LoadOptions] to be a [Params].
     * */
    override fun getParams(): Params {
        val rowFormat = "%40s: %s"
        return optionFields.filter { it.annotations.any { it is Parameter } }
                .onEach { it.isAccessible = true }
                .associate { "-${it.name}" to it.get(this) }
                .filter { it.value != null }
                .let { Params.of(it).withRowFormat(rowFormat) }
    }

    /**
     * Convert the [LoadOptions] to be a string.
     * The operation should be reversible:
     *
     * val args = "..."
     * val options1 = LoadOptions.parse(args)
     * val normalizedArgs = options1
     * val options2 = LoadOptions.parse(normalizedArgs)
     * require(normalizedArgs == options2.toString())
     * */
    override fun toString(): String {
        return modifiedParams.distinct().sorted()
                .withCmdLineStyle(true)
                .withKVDelimiter(" ")
                .withDistinctBooleanParams(arity1BooleanParams)
                .formatAsLine().replace("\\s+".toRegex(), " ")
    }

    /**
     * The equality check, two [LoadOptions] are equal only when the normalized arguments string are equal.
     * */
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        return other is LoadOptions && other.toString() == toString()
    }

    // TODO: hashCode can not rely on any member filed because static filed defaultParams uses hashCode before
    override fun hashCode(): Int {
        return super.hashCode()
    }

    /**
     * Create a new [LoadOptions] object.
     * */
    open fun clone() = parse(toString(), this)

    /**
     * Correct [outLinkSelector].
     * There is a JCommand bug with quoted options.
     * */
    private fun correctOutLinkSelector(): String? {
        return outLinkSelector.trim('"')
            .takeIf { it.isNotBlank() }
            ?.let { appendSelectorIfMissing(it, "a") }
    }

    private fun doRefresh(value: Boolean) {
        if (value) {
            expires = Duration.ZERO
            expireAt = Instant.now()

            itemExpires = Duration.ZERO
            itemExpireAt = Instant.now()

            ignoreFailure = true
        }
        refresh = value
    }

    companion object {
        /**
         * The default option.
         * */
        val DEFAULT = LoadOptions("", VolatileConfig.DEFAULT)

        /**
         * A list of all option fields.
         * */
        val optionFields = LoadOptions::class.java.declaredFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .filter { it.annotations.filterIsInstance<Parameter>().isNotEmpty() }
            .onEach {
                val name = it.name
                val count = it.annotations.filterIsInstance<Parameter>().count { it.names.contains("-$name") }
                require(count > 0) { "Missing -$name option for field <$name>. " +
                            "Every option with name `optionName` has to take a [Parameter] name [-optionName]." }
            }

        /**
         * A map of all option fields.
         * */
        val optionFieldsMap = optionFields.associateBy { it.name }

        /**
         * A map of all default options.
         * */
        val defaultParams = optionFields.associate { it.name to it.get(DEFAULT) }

        /**
         * A map of all default options.
         * */
        val defaultArgsMap = DEFAULT.toArgsMap()

        /**
         * A list of the options who's arity is 0.
         * */
        val arity0BooleanParams = optionFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .filter { it.get(DEFAULT) is Boolean }
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .filter { it.arity < 1 }
            .flatMap { it.names.toList() }
            .toList()

        /**
         * A list of the options who's arity is 1.
         * */
        val arity1BooleanParams = optionFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .filter { it.get(DEFAULT) is Boolean }
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .filter { it.arity == 1 }
            .flatMap { it.names.toList() }
            .toList()

        /**
         * A list of all the option names.
         * */
        val optionNames = optionFields
            .asSequence()
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .flatMap { it.names.toList() }
            .toList()

        /**
         * A list of all the names of options who are allowed with REST APIs.
         * */
        val apiPublicOptionNames = optionFields
            .asSequence()
            .filter { it.kotlinProperty?.hasAnnotation<ApiPublic>() == true }
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .flatMap { it.names.toList() }
            .toList()

        /**
         * Generate the help message from the field annotations.
         * */
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

        /**
         * Set the field value who has an annotation [annotationName].
         * */
        fun setFieldByAnnotation(options: LoadOptions, annotationName: String, value: Any) {
            optionFields.forEach {
                val found = it.annotations.filterIsInstance<Parameter>().any { annotationName in it.names }
                if (found) {
                    it.isAccessible = true
                    it.set(options, value)
                }
            }
        }

        /**
         * Get all the available option names for field [fieldName].
         * */
        fun getOptionNames(fieldName: String): List<String> {
            return optionFields
                .asSequence()
                .filter { it.name == fieldName }
                .flatMap { it.annotations.toList() }
                .filterIsInstance<Parameter>()
                .flatMap { it.names.toList() }
                .toList()
        }

        /**
         * Create an empty [LoadOptions].
         * */
        fun create(conf: VolatileConfig) = LoadOptions(arrayOf(), conf).apply { parse() }

        /**
         * Normalize [args], all option names in a normalized argument string match the field name in [LoadOptions].
         * */
        fun normalize(args: String) = parse(args, VolatileConfig.UNSAFE).toString()

        /**
         * Parse the [args] with other [conf].
         * */
        fun parse(args: String, conf: VolatileConfig) = LoadOptions(args.trim(), conf).apply { parse() }

        /**
         * Parse the [args] with other [options].
         * */
        fun parse(args: String, options: LoadOptions) = LoadOptions(args.trim(), options).apply { parse() }

        /**
         * Create a new LoadOptions with [o1] and [o2]'s items, [o2] overrides [o1].
         * */
        fun merge(o1: LoadOptions, o2: LoadOptions) = parse("$o1 $o2", o2)

        /**
         * Create a new LoadOptions with [o1] and [args], [args] overrides [o1].
         * */
        fun merge(o1: LoadOptions, args: String?) = parse("$o1 $args", o1)

        /**
         * Create a new LoadOptions with [args] and [args2], [args2] overrides [args].
         * */
        fun merge(args: String?, args2: String?, conf: VolatileConfig) = parse("$args $args2", conf)

        /**
         * Erase the specified option, the option name has to match the field name in LoadOptions.
         * */
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
