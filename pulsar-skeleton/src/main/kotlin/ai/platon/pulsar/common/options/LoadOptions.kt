package ai.platon.pulsar.common.options

import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.PageEvent
import ai.platon.pulsar.crawl.event.impl.PageEventHandlersFactory
import ai.platon.pulsar.dom.select.appendSelectorIfMissing
import ai.platon.pulsar.persist.metadata.FetchMode
import com.beust.jcommander.Parameter
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.kotlinProperty

/**
 * Load options are a set of parameters that define how a page should be loaded, including any specific
 * behaviors or conditions that should be applied during the loading process.
 *
 * The load options, or load arguments, can be a plain string in the form of command line parameters,
 * and can be parsed into a [LoadOptions] object.
 *
 * ```kotlin
 * // parse a string into a LoadOptions object
 * val options = session.options('-expires 1d -itemExpires 1d -ignoreFailure -parse -storeContent')
 * // fetch after 1d since last fetch
 * session.load('https://www.jd.com', '-expires 1d')
 * // fetch immediately ignoring errors
 * session.load('https://www.jd.com', '-refresh')
 * // do not fetch after dead time
 * session.load('https://www.jd.com', '-deadline 2022-04-15T18:36:54.941Z')
 * // activate the parse phase
 * session.load('https://www.jd.com', '-parse')
 * // write the page content into storage
 * session.load('https://www.jd.com', '-storeContent')
 * ```
 * */
open class LoadOptions(
    argv: Array<String>,
    val conf: VolatileConfig,
    var rawEvent: PageEvent? = null,
    var rawItemEvent: PageEvent? = null,
    var referrer: String? = null,
) : CommonOptions(argv) {

    /**
     * The entity name of the page, for example, article, product, hotel, flower, etc., it's optional.
     * */
    @ApiPublic
    @Parameter(
        names = ["-e", "-entity", "--entity"],
        description = "The entity of the page, it's optional."
    )
    var entity = ""

    /**
     * The task label is optional and may be utilized to categorize tasks into groups.
     * */
    @ApiPublic
    @Parameter(
        names = ["-l", "-label", "--label"],
        description = "The task label, it's optional and can be used to group tasks"
    )
    var label = ""

    /**
     * The taskId is optional and serves to differentiate tasks if needed.
     * */
    @ApiPublic
    @Parameter(
        names = ["-taskId", "--task-id"],
        description = "The taskId is optional and serves to differentiate tasks if needed."
    )
    var taskId = ""

    /**
     * Task time is usually used to denote the name of a batch of tasks.
     *
     * The task time is initialized to Instant.EPOCH, so parse() and toString() operations are symmetric.
     * */
    @ApiPublic
    @Parameter(
        names = ["-taskTime", "--task-time"], converter = InstantConverter::class,
        description = "The taskTime is usually used to denote the name of a batch of tasks."
    )
    var taskTime = Instant.EPOCH

    /**
     * The task's deadline indicates the time by which it should be completed. If this deadline is surpassed,
     * the task must be promptly discarded.
     * */
    @ApiPublic
    @Parameter(
        names = ["-deadline", "--deadline"], converter = InstantConverter::class,
        description = "The task's deadline indicates the time by which it should be completed. If this deadline is surpassed, " +
                " the task must be promptly discarded."
    )
    var deadline = DateTimes.doomsday

    /**
     * The auth token, used for authorization purpose.
     * */
    @ApiPublic
    @Parameter(
        names = ["-authToken", "--auth-token"],
        description = "The auth token, can be used for authorization purpose."
    )
    var authToken = ""

    /**
     * Specify whether the load execution is read-only or not. When a load execution is read-only, it ensures that the
     * webpage loaded remains unchanged by the execution.
     * */
    @ApiPublic
    @Parameter(
        names = ["-readonly"],
        description = "Specify whether the load execution is read-only or not. " +
                "When a load execution is read-only, it ensures that the webpage loaded remains unchanged by the execution."
    )
    var readonly = false

    /**
     * If true, fetch the url as a resource without browser rendering.
     * */
    @ApiPublic
    @Parameter(
        names = ["-resource", "-isResource"],
        description = "If true, fetch the url as a resource without browser rendering."
    )
    var isResource = false

    /**
     * The expiry duration. If the expiry time is exceeded, the page should be fetched from the Internet.
     *
     * The term "expires" usually be used for an expiry time, for example, http-equiv, or in cookie specification,
     * guess it means "expires at".
     *
     * The expires field supports both ISO-8601 standard and hadoop time duration format:
     * 1. ISO-8601 standard : PnDTnHnMn.nS
     * 2. Hadoop time duration format: 100s, 1m, 1h, 1d, valid units are : ns, us, ms, s, m, h, d.
     * */
    @ApiPublic
    @Parameter(
        names = ["-i", "-expires", "--expires"], converter = DurationConverter::class,
        description = "The expiry duration. " +
                "If the expiry time is exceeded, the page should be fetched from the Internet."
    )
    var expires = LoadOptionDefaults.expires

    /**
     * The expiry time point. If the expiry time is exceeded, the page should be fetched from the Internet.
     *
     * Accept the following format:
     * 1. yyyy-MM-dd[ HH[:mm[:ss]]]
     * 2. ISO_INSTANT, or yyyy-MM-ddTHH:mm:ssZ
     * */
    @ApiPublic
    @Parameter(
        names = ["-expireAt", "--expire-at"], converter = InstantConverter::class,
        description = "The expiry time point. " +
                "If the expiry time is exceeded, the page should be fetched from the Internet."
    )
    var expireAt = LoadOptionDefaults.expireAt

    /**
     * The selector to extract links in portal pages.
     * */
    @ApiPublic
    @Parameter(
        names = ["-ol", "-outLink", "-outLinkSelector", "--out-link-selector", "-outlink", "-outlinkSelector", "--outlink-selector"],
        description = "The selector to extract links in portal pages."
    )
    var outLinkSelector = ""

    /**
     * The pattern to select out links in the portal page.
     * */
    @ApiPublic
    @Parameter(
        names = ["-olp", "-outLinkPattern", "--out-link-pattern"],
        description = "The pattern to select out links in the portal page"
    )
    var outLinkPattern = ".+"

    /**
     * The selector for element to click.
     * TODO: not implemented yet
     * */
    @ApiPublic
    @Parameter(
        names = ["-click", "-clickTarget", "--click-target"],
        description = "The selector for element to click."
    )
    var clickTarget = ""

    /**
     * The selector for next page anchor.
     * TODO: not implemented yet
     * */
    @ApiPublic
    @Parameter(
        names = ["-np", "-nextPage", "-nextPageSelector", "--next-page-selector"],
        description = "The css selector of next page anchor"
    )
    var nextPageSelector = ""

    /**
     * The iframe id to switch to.
     * TODO: not implemented yet
     * */
    @ApiPublic
    @Parameter(names = ["-ifr", "-iframe", "--iframe"], description = "The iframe id to switch to")
    var iframe = 0

    /**
     * Specify how many links to extract for out pages.
     * */
    @ApiPublic
    @Parameter(
        names = ["-tl", "-topLinks", "--top-links"],
        description = "Specify how many links to extract for out pages."
    )
    var topLinks = 20

    /**
     * Choose the top N anchor groups for further process. Used by auto web mining project.
     * */
    @ApiPublic
    @Parameter(
        names = ["-tng", "-topNAnchorGroups", "--top-anchor-groups"],
        description = "Try the top N anchor groups"
    )
    var topNAnchorGroups = 3

    /**
     * The selector specified element should have a non-blank text, the system should
     * wait until the element is filled by a non-blank text, or until it times out.
     * TODO: not implemented yet
     * */
    @ApiPublic
    @Parameter(
        names = ["-wnb", "-waitNonBlank"],
        description = "The selector specified element should have a non-blank text"
    )
    var waitNonBlank: String = ""

    /**
     * The selector specified element should have a non-blank text, the task should
     * be retried if the element's text content is empty or blank.
     * TODO: not implemented yet
     * */
    @ApiPublic
    @Parameter(
        names = ["-rnb", "-requireNotBlank"],
        description = "The selector specified element should have a non-blank text"
    )
    var requireNotBlank: String = ""

    /**
     * The minimum page size expected, if it is less than that, it will need to be re-fetched.
     *
     * The unit is byte.
     * */
    @ApiPublic
    @Parameter(
        names = ["-rs", "-requireSize", "--require-size"],
        description = "The minimum page size expected"
    )
    var requireSize = 0

    /**
     * The minimum number of images expected in the page, if it is less than that,
     * it will need to be re-fetched.
     * */
    @ApiPublic
    @Parameter(
        names = ["-ri", "-requireImages", "--require-images"],
        description = "The minimum number of images expected in the page"
    )
    var requireImages = 0

    /**
     * The minimum number of anchors expected in the page, if it is less than that,
     * it will need to be re-fetched.
     * */
    @ApiPublic
    @Parameter(
        names = ["-ra", "-requireAnchors", "--require-anchors"],
        description = "The minimum number of anchors expected in the page"
    )
    var requireAnchors = 0

    /**
     * The fetch mode.
     *
     * Only BROWSER is supported currently.
     * */
    @Parameter(
        names = ["-fm", "-fetchMode", "--fetch-mode"], converter = FetchModeConverter::class,
        description = "The fetch mode"
    )
    var fetchMode = FetchMode.BROWSER

    /**
     * Specify which browser to use, google chrome is the default.
     * TODO: session scope browser choice is not support by now
     * */
    @Parameter(
        names = ["-b", "-browser", "--browser"], converter = BrowserTypeConverter::class,
        description = "Specify which browser to use, google chrome is the default"
    )
    var browser = LoadOptionDefaults.browser

    /**
     * The number of times the page should be scrolled down after it has just been opened in a browser.
     * */
    @Parameter(
        names = ["-sc", "-scrollCount", "--scroll-count"],
        description = "The count to scroll down after a page being opened in a browser"
    )
    var scrollCount = InteractSettings.DEFAULT.scrollCount

    /**
     * The interval to scroll down.
     * */
    @Parameter(
        names = ["-si", "-scrollInterval", "--scroll-interval"], converter = DurationConverter::class,
        description = "The interval to scroll down after a page being opened in a browser"
    )
    var scrollInterval = InteractSettings.DEFAULT.scrollInterval

    /**
     * The maximum time to perform javascript injected into the browser.
     * */
    @Parameter(
        names = ["-stt", "-scriptTimeout", "--script-timeout"], converter = DurationConverter::class,
        description = "The maximum time to perform javascript injected into the browser"
    )
    var scriptTimeout = InteractSettings.DEFAULT.scriptTimeout

    /**
     * The maximum time to wait for a page to finish.
     * */
    @Parameter(
        names = ["-plt", "-pageLoadTimeout", "--page-load-timeout"], converter = DurationConverter::class,
        description = "The maximum time to wait for a page to finish"
    )
    var pageLoadTimeout = InteractSettings.DEFAULT.pageLoadTimeout

    /**
     * The browser used to visit the item pages.
     * TODO: session scope browser choice is not support by now
     * */
    @Parameter(
        names = ["-ib", "-itemBrowser", "--item-browser"], converter = BrowserTypeConverter::class,
        description = "The browser used to visit the item pages"
    )
    var itemBrowser = LoadOptionDefaults.browser

    /**
     * The same as expires, but only works for item pages.
     * */
    @ApiPublic
    @Parameter(
        names = ["-ii", "-itemExpires", "--item-expires"], converter = DurationConverter::class,
        description = "The same as expires, but only works for item pages"
    )
    var itemExpires = ChronoUnit.DECADES.duration

    /**
     * If an item page is expired, it should be fetched from the web again.
     * */
    @ApiPublic
    @Parameter(
        names = ["-itemExpireAt", "--item-expire-at"], converter = InstantConverter::class,
        description = "If an item page is expired, it should be fetched from the web again"
    )
    var itemExpireAt = DateTimes.doomsday

    /**
     * The same as scrollCount, but only works for item pages.
     * */
    @Parameter(
        names = ["-isc", "-itemScrollCount", "--item-scroll-count"],
        description = "The same as scrollCount, but only works for item pages"
    )
    var itemScrollCount = scrollCount

    /**
     * The same as scrollInterval, but only works for item pages.
     * */
    @Parameter(
        names = ["-isi", "-itemScrollInterval", "--item-scroll-interval"], converter = DurationConverter::class,
        description = "The same as scrollInterval, but only works for item pages"
    )
    var itemScrollInterval = scrollInterval

    /**
     * The same as scriptTimeout, but only works for item pages.
     * */
    @Parameter(
        names = ["-ist", "-itemScriptTimeout", "--item-script-timeout"], converter = DurationConverter::class,
        description = "The same as scriptTimeout, but only works for item pages"
    )
    var itemScriptTimeout = scriptTimeout

    /**
     * The same as pageLoadTimeout, but only works for item pages.
     * */
    @Parameter(
        names = ["-iplt", "-itemPageLoadTimeout", "--item-page-load-timeout"], converter = DurationConverter::class,
        description = "The same as pageLoadTimeout, but only works for item pages"
    )
    var itemPageLoadTimeout = pageLoadTimeout

    /**
     * Re-fetch the item pages if the required text is blank.
     * */
    @ApiPublic
    @Parameter(
        names = ["-irnb", "-itemRequireNotBlank", "--item-require-not-blank"],
        description = "Re-fetch the item pages if the required text is blank"
    )
    var itemRequireNotBlank = ""

    /**
     * Re-fetch item pages smaller than requireSize.
     * */
    @ApiPublic
    @Parameter(
        names = ["-irs", "-itemRequireSize", "--item-require-size"],
        description = "Re-fetch item pages smaller than requireSize"
    )
    var itemRequireSize = 0

    /**
     * Re-fetch item pages whose images is less than requireImages.
     * */
    @ApiPublic
    @Parameter(
        names = ["-iri", "-itemRequireImages", "--item-require-images"],
        description = "Re-fetch item pages who's images is less than requireImages"
    )
    var itemRequireImages = 0

    /**
     * Re-fetch item pages with fewer anchors than the required number specified by requireAnchors.
     * */
    @ApiPublic
    @Parameter(
        names = ["-ira", "-itemRequireAnchors", "--item-require-anchors"],
        description = "Re-fetch item pages who's anchors is less than requireAnchors"
    )
    var itemRequireAnchors = 0

    /**
     * Persist fetched pages as soon as possible.
     * */
    @Parameter(
        names = ["-persist", "--persist"], arity = 1,
        description = "Persist fetched pages as soon as possible"
    )
    var persist = true

    /**
     * If false, do not persist the page content which is usually very large.
     * */
    @Parameter(
        names = ["-sct", "-storeContent", "--store-content"], arity = 1,
        description = "If false, do not persist the page content which is usually very large."
    )
    var storeContent = LoadOptionDefaults.storeContent

    /**
     * If the option is set, do not persist the page content which is usually very large.
     * If the option is true, it overrides [storeContent].
     * */
    @Parameter(
        names = ["-dct", "-dropContent", "--drop-content"], arity = 1,
        description = "If the option exists, do not persist the page content which is usually very large."
    )
    var dropContent = false

    /**
     * If false, load the page without the content which is usually very large
     * TODO: review the design
     * */
//    @Parameter(names = ["-lct", "-loadContent", "--load-content"], arity = 1,
//        description = "If false, load the page without its content which is usually very large")
//    var loadContent = LoadOptionDefaults.loadContent

    /**
     * Refresh the fetch state of a page, clear the retry counters.
     * If true, the page should be fetched, just like we click the refresh button on a real browser.
     * The option can be explained as follows: -refresh = -ignoreFailure -i 0s and set page.fetchRetries = 0.
     *
     * TODO: consider add an option itemRefresh
     * */
    @ApiPublic
    @Parameter(
        names = ["-refresh", "--refresh"],
        description = "Refresh the fetch state of a page, clear the retry counters." +
                " If true, the page should be fetched immediately." +
                " The option can be explained as follows:" +
                " -refresh = -ignoreFailure -i 0s and set page.fetchRetries = 0"
    )
    var refresh = false
        set(value) {
            field = doRefresh(value)
        }

    /**
     * Retry fetching the page even if it's failed last time.
     * */
    @ApiPublic
    @Parameter(
        names = ["-ignF", "-ignoreFailure", "--ignore-failure"],
        description = "Retry fetching the page even if it's failed last time"
    )
    var ignoreFailure = LoadOptionDefaults.ignoreFailure

    /**
     * Retry to fetch at most n times, if page.fetchRetries > nMaxRetry,
     * the page is marked as gone and do not fetch it again until -refresh is set to clear page.fetchRetries
     * */
    @Parameter(
        names = ["-nmr", "-nMaxRetry", "--n-max-retry"],
        description = "Retry to fetch at most n times, if page.fetchRetries > nMaxRetry," +
                " the page is marked as gone and do not fetch it again until -refresh is set to clear page.fetchRetries"
    )
    var nMaxRetry = 3

    /**
     * Retry at most n times at fetch phase immediately if RETRY(1601) code return.
     * */
    @Parameter(
        names = ["-njr", "-nJitRetry", "--n-jit-retry"],
        description = "Retry at most n times at fetch phase immediately if RETRY(1601) code return"
    )
    var nJitRetry = LoadOptionDefaults.nJitRetry

    /**
     * If false, pages are flushed into database as soon as possible.
     * */
    @Parameter(
        names = ["-lazyFlush", "--lazy-flush"],
        description = "If false, pages are flushed into database as soon as possible"
    )
    var lazyFlush = LoadOptionDefaults.lazyFlush

    /**
     * Run browser in incognito mode.
     * Not used since the browser always running in temporary contexts.
     * */
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
    @Parameter(
        names = ["-hardRedirect", "--hard-redirect"],
        description = "If false, return the original page record but the redirect target's content," +
                " otherwise, return the page record of the redirected target." +
                " If we use a browser, redirections are handled by the browser so the flag is ignored."
    )
    var hardRedirect = false

    /**
     * If true, parse the page when it's just be fetched.
     * */
    @Parameter(names = ["-ps", "-parse", "--parse"], description = "If true, parse the page when it's just be fetched.")
    var parse = LoadOptionDefaults.parse

    /**
     * Reparse links if the page has been parsed before.
     * */
    @Parameter(
        names = ["-rpl", "-reparseLinks", "--reparse-links"],
        description = "Re-parse links if the page has been parsed before."
    )
    var reparseLinks = false

    /**
     * If true, remove the query parameters in the url.
     * */
    @Parameter(
        names = ["-ignoreUrlQuery", "--ignore-url-query"],
        description = "Remove the query parameters in the url"
    )
    var ignoreUrlQuery = false

    /**
     * If true, no normalizer will be applied when parse links.
     * */
    @Parameter(
        names = ["-noNorm", "--no-link-normalizer"],
        description = "If true, no normalizer will be applied when parse links."
    )
    var noNorm = false

    /**
     * If true, no filter will be applied when parse links.
     * */
    @Parameter(
        names = ["-noFilter", "--no-link-filter"],
        description = "If true, no filter will be applied when parse links."
    )
    var noFilter = false

    /**
     * Indicates the network condition.
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
    @Parameter(
        names = ["-test", "--test"],
        description = "The test level, 0 to disable, we will talk more in test mode"
    )
    var test = LoadOptionDefaults.test

    /**
     * The load option version.
     * */
    @Parameter(names = ["-v", "-version", "--version"], description = "The load option version")
    var version = "20220918"

    /**
     * Get the corrected [outLinkSelector] or null. See [outLinkSelector] for more information.
     * */
    val outLinkSelectorOrNull
        get() = outLinkSelector.takeIf { it.isNotBlank() }

    val event: PageEvent get() = enableEvent()

    val itemEvent: PageEvent get() = enableItemEvent()

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
            this(split(args), other.conf, other.rawEvent, other.rawItemEvent, other.referrer)

    /**
     * Parse the arguments into [LoadOptions] with JCommander and with bug fixes.
     * */
    override fun parse(): Boolean {
        val b = super.parse()
        if (b) {
            // fix zero-arity boolean parameter overwriting
            optionFields
                .filter { arity0BooleanParams.contains("-${it.name}") }
                .filter { argv.contains("-${it.name}") }
                .forEach {
                    it.isAccessible = true
                    it.set(this, true)
                }
            // fix out link parsing (remove surrounding symbols)
            outLinkSelector = correctOutLinkSelector() ?: ""
        }
        return b
    }

    /**
     * Create a new [LoadOptions] object for item pages.
     * */
    open fun createItemOptions(): LoadOptions {
        val itemOptions = clone()
        itemOptions.itemOptions2MajorOptions()

        if (itemOptions.browser == BrowserType.NATIVE) {
            itemOptions.fetchMode = FetchMode.NATIVE
        }
        itemOptions.rawEvent = rawItemEvent

        return itemOptions
    }

    /**
     * Check if the page expires.
     *
     * A page is expired when:
     * 1. the last fetch time is before [expireAt] and now is after [expireAt]
     * 2. (the last fetch time + [expires]) is exceeded
     * */
    fun isExpired(prevFetchTime: Instant): Boolean {
        val now = Instant.now()
        return when {
            refresh -> true
            expireAt in prevFetchTime..now -> true
            now >= prevFetchTime + expires -> true
            else -> false
        }
    }

    /**
     * If the page is dead, drop the task as soon as possible.
     * */
    fun isDead(): Boolean {
        return deadline < Instant.now()
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

        rawEvent = rawItemEvent
    }

    /**
     * Write option values to [conf].
     *
     * [LoadOptions] is not globally visible, we have to pass values to modules which can not see it
     * through a [VolatileConfig] object.
     * */
    fun overrideConfiguration() = overrideConfiguration(this.conf)

    /**
     * Write option values to [conf].
     *
     * [LoadOptions] is not globally visible, we have to pass values to modules which can not see it
     * through a [VolatileConfig] object.
     * */
    fun overrideConfiguration(conf: VolatileConfig?): VolatileConfig? = conf?.apply {
        setInteractionSettings()

        rawEvent?.let { putBean(it) }
        setEnum(CapabilityTypes.BROWSER_TYPE, browser)
        // incognito mode is never used because the browsers are always running in temporary contexts
        setBoolean(CapabilityTypes.BROWSER_INCOGNITO, incognito)
    }
    
    private fun setInteractionSettings() {
        val modified = listOf(
            "netCondition",
            "scrollCount",
            "scrollInterval",
            "scriptTimeout",
            "pageLoadTimeout"
        ).any { !isDefault(it) }
        
        if (!modified) {
            return
        }

        val interactSettings = when (netCondition) {
            Condition.WORSE -> InteractSettings.WORSE_NET_SETTINGS
            Condition.WORST -> InteractSettings.WORST_NET_SETTINGS
            else -> InteractSettings.GOOD_NET_SETTINGS
        }.copy()

        interactSettings.scrollCount = scrollCount
        interactSettings.scrollInterval = scrollInterval
        interactSettings.scriptTimeout = scriptTimeout
        interactSettings.pageLoadTimeout = pageLoadTimeout

        interactSettings.overrideConfiguration(conf)
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
     * Convert the [LoadOptions] to a string.
     * The two operations [parse] and [toString] are reversible:
     *
     * ```
     * val args = "..."
     * val options1 = LoadOptions.parse(args)
     * val normalizedArgs = options1
     * val options2 = LoadOptions.parse(normalizedArgs)
     * require(normalizedArgs == options2.toString())
     * ```
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
        if (this === other) {
            return true;
        }

        return other is LoadOptions && other.toString() == toString()
    }

    // TODO: hashCode can not rely on any member filed because static filed defaultParams uses hashCode before
    override fun hashCode(): Int {
        return super.hashCode()
    }

    /**
     * Create a new [LoadOptions] object with the same arguments string and event handlers.
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

    private fun doRefresh(value: Boolean): Boolean {
        if (value) {
            expires = Duration.ZERO
            expireAt = Instant.now()

            itemExpires = Duration.ZERO
            itemExpireAt = Instant.now()

            ignoreFailure = true
        }
        return value
    }

    /**
     * Ensure [event] is created.
     * */
    private fun enableEvent(): PageEvent {
        val eh = rawEvent ?: PageEventHandlersFactory(conf).create()
        rawEvent = eh
        return eh
    }

    /**
     * Ensure [rawItemEvent] is created.
     * */
    private fun enableItemEvent(): PageEvent {
        val eh = rawEvent ?: PageEventHandlersFactory(conf).create()
        rawItemEvent = eh
        return eh
    }

    companion object {
        /**
         * The default option.
         * */
        val DEFAULT = LoadOptions("", VolatileConfig.UNSAFE)

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
                require(count > 0) {
                    "Missing -$name option for field <$name>. " +
                            "Every option with name `optionName` has to take a [Parameter] name [-optionName]."
                }
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
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .flatMap { it.names.toList() }
            .toList()

        /**
         * A list of all the names of options who are allowed with REST APIs.
         * */
        val apiPublicOptionNames = optionFields
            .filter { it.kotlinProperty?.hasAnnotation<ApiPublic>() == true }
            .flatMap { it.annotations.toList() }
            .filterIsInstance<Parameter>()
            .flatMap { it.names.toList() }
            .toList()

        /**
         * Generate the help message from the field annotations.
         * */
        val helpList: List<List<String>>
            get() =
                optionFields
                    .mapNotNull { (it.annotations.firstOrNull { it is Parameter } as? Parameter)?.to(it) }
                    .map {
                        listOf(
                            it.first.names.joinToString { it },
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
         * Create an empty [LoadOptions].
         * */
        fun createUnsafe() = create(VolatileConfig.UNSAFE)

        /**
         * Normalize [args], all option names in a normalized argument string match the field name in [LoadOptions].
         * */
        fun normalize(vararg args: String?) = parse(args.filterNotNull().joinToString(" ")).toString()

        /**
         * Parse the [args] with other [conf].
         * */
        fun parse(args: String, conf: VolatileConfig = VolatileConfig()) =
            LoadOptions(args.trim(), conf).apply { parse() }

        /**
         * Parse the [args] with other [options].
         * */
        fun parse(args: String, options: LoadOptions) = LoadOptions(args.trim(), options).apply {
            referrer = options.referrer
            parse()
        }

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

/**
 * The default load options, be careful if you have to change the default behaviour.
 * */
object LoadOptionDefaults {
    /**
     * The default expiry time, some time we may need expire all pages by default, for example, in test mode
     * */
    val EXPIRES = ChronoUnit.DECADES.duration
    
    /**
     * The default time to expire
     * */
    val EXPIRE_AT = DateTimes.doomsday
    
    /**
     * Lazy flush.
     * */
    const val LAZY_FLUSH = true
    
    /**
     * Trigger the parse phase or not.
     *
     * Do not parse by default, since there are may ways to trigger a webpage parsing:
     * 1. use session.parse()
     * 2. add a -parse option
     * 3. use a [ai.platon.pulsar.crawl.common.url.ParsableHyperlink]
     * */
    const val PARSE = false
    
    /**
     * Store webpage content or not.
     *
     * Store webpage content by default.
     * If we are running a public cloud, this option might be changed to false.
     * */
    const val STORE_CONTENT = true
    /**
     * Load webpage content or not.
     *
     * Load webpage content by default.
     * If we are running a public cloud, this option might be changed to false.
     *
     * TODO: review the design
     * */
//    var loadContent = true
    /**
     * If true, still fetch the page even if it is gone.
     * */
    const val IGNORE_FAILURE = false
    
    /**
     * There are several cases to enable jit retry.
     * For example, in a test environment.
     * */
    const val N_JIT_RETRY = -1
    
    /**
     * The default browser is chrome with pulsar implemented web driver.
     * */
    val BROWSER = BrowserType.PULSAR_CHROME
    
    /**
     * Set to be > 0 if we are doing unit test or other test.
     * We will talk more, log more and trace more in test mode.
     * */
    const val TEST = 0
    
    /**
     * The default expiry time, some time we may need expire all pages by default, for example, in test mode
     * */
    var expires = EXPIRES

    /**
     * The default time to expire
     * */
    var expireAt = EXPIRE_AT

    /**
     * Lazy flush.
     * */
    var lazyFlush = LAZY_FLUSH

    /**
     * Trigger the parse phase or not.
     *
     * Do not parse by default, since there are may ways to trigger a webpage parsing:
     * 1. use session.parse()
     * 2. add a -parse option
     * 3. use a [ai.platon.pulsar.crawl.common.url.ParsableHyperlink]
     * */
    var parse = PARSE

    /**
     * Store webpage content or not.
     *
     * Store webpage content by default.
     * If we are running a public cloud, this option might be changed to false.
     * */
    var storeContent = STORE_CONTENT
    /**
     * Load webpage content or not.
     *
     * Load webpage content by default.
     * If we are running a public cloud, this option might be changed to false.
     *
     * TODO: review the design
     * */
//    var loadContent = true
    /**
     * If true, still fetch the page even if it is gone.
     * */
    var ignoreFailure = IGNORE_FAILURE

    /**
     * There are several cases to enable jit retry.
     * For example, in a test environment.
     * */
    var nJitRetry = N_JIT_RETRY

    /**
     * The default browser is chrome with pulsar implemented web driver.
     * */
    var browser = BROWSER

    /**
     * Set to be > 0 if we are doing unit test or other test.
     * We will talk more, log more and trace more in test mode.
     * */
    var test = TEST
    
    /**
     * Reset all the options to default.
     * */
    fun reset() {
        expires = EXPIRES
        expireAt = EXPIRE_AT
        lazyFlush = LAZY_FLUSH
        parse = PARSE
        storeContent = STORE_CONTENT
        ignoreFailure = IGNORE_FAILURE
        nJitRetry = N_JIT_RETRY
        browser = BROWSER
        test = TEST
    }
}
