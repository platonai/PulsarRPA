package ai.platon.pulsar.crawl.common.url

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.options.findOption
import ai.platon.pulsar.common.urls.StatefulHyperlink
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.*
import ai.platon.pulsar.persist.WebPage
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

open class ListenableHyperlink(
        /**
         * The url of this hyperlink
         * */
        url: String,
        /**
         * The anchor text of this hyperlink
         * */
        text: String = "",
        /**
         * The order of this hyperlink in it's referer page
         * */
        order: Int = 0,
        /**
         * The url of the referer page
         * */
        referer: String? = null,
        /**
         * The url arguments
         * */
        args: String? = null,
        /**
         * A click url is a url variant, it's the raw url in the html without normalization,
         * for example, an url with a timestamp query parameter added
         * */
        href: String? = null
): StatefulHyperlink(url, text, order, referer, args, href) {

    override val isPersistable: Boolean = false

    val idleTime get() = Duration.between(modifiedAt, Instant.now())

    open var loadEventHandler: LoadEventPipelineHandler = DefaultLoadEventHandler()
    open var jsEventHandler: JsEventHandler? = DefaultJsEventHandler()
    open var crawlEventHandler: CrawlEventHandler? = DefaultCrawlEventHandler()
}

open class CompletableHyperlink(
    override var url: String,
    override var args: String? = null,
    override var referer: String? = null,
    override var href: String? = null
): UrlAware, Comparable<UrlAware>, CompletableFuture<WebPage>() {

    override val configuredUrl get() = if (args != null) "$url $args" else url

    override val isNil: Boolean get() = url == AppConstants.NIL_PAGE_URL

    /**
     * If this link is persistable
     * */
    override val isPersistable: Boolean = false

    override val label: String get() = findOption(args, listOf("-l", "-label", "--label")) ?: ""

    /**
     * Required website language
     * */
    override var lang: String = "*"
    /**
     * Required website country
     * */
    override var country: String = "*"
    /**
     * Required website district
     * */
    override var district: String = "*"
    /**
     * The maximum retry times
     * */
    override var maxRetry: Int = 3

    override val deadTime: Instant
        get() {
            val deadTime = findOption(args, listOf("-deadTime", "--dead-time")) ?: ""
            return DateTimes.parseBestInstantOrNull(deadTime) ?: DateTimes.doomsday
        }

    open var crawlEventHandler: CrawlEventHandler? = DefaultCrawlEventHandler().also {
        it.onAfterLoadPipeline.addLast { _, page -> complete(page) }
    }

    /**
     * A abstract url can be compare to one of the following types:
     * 1. a [String]
     * 2. a [URL]
     * 3. a [UrlAware]
     * */
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return when (other) {
            is String -> url == other
            is URL -> url == other.toString()
            is UrlAware -> url == other.url
            else -> false
        }
    }

    override fun hashCode() = url.hashCode()

    override fun compareTo(other: UrlAware): Int {
        return url.compareTo(other.url)
    }

    override fun toString() = url
}
