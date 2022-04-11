package ai.platon.pulsar.crawl.common.url

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.options.findOption
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.StatefulHyperlink
import ai.platon.pulsar.common.urls.StatefulUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.*
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Document
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer

interface ListenableHyperlink: UrlAware {
    val eventHandler: PulsarEventHandler
}

open class StatefulListenableHyperlink(
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
): StatefulHyperlink(url, text, order, referer, args, href), ListenableHyperlink {

    override val isPersistable: Boolean = false

    val idleTime get() = Duration.between(modifiedAt, Instant.now())

    override var eventHandler: PulsarEventHandler = DefaultPulsarEventHandler()
}

open class ParsableHyperlink(
    /**
     * The url of this hyperlink
     * */
    url: String,
    val onParse: (WebPage, Document) -> Unit
): Hyperlink(url, args = "-parse"), ListenableHyperlink {

    /**
     * Java compatible constructor
     * */
    constructor(url: String, onParse: BiConsumer<WebPage, Document>):
            this(url, { page, document -> onParse.accept(page, document) })

    override var eventHandler: PulsarEventHandler = DefaultPulsarEventHandler().also {
        it.loadEventHandler.onAfterHtmlParse.addLast(object: HtmlDocumentHandler() {
            override fun invoke(page: WebPage, document: FeaturedDocument) = onParse(page, document.document)
        })
    }
}

open class CompletableHyperlink<T>(
    /**
     * The url of this hyperlink
     * */
    override var url: String,
    /**
     * The anchor text of this hyperlink
     * */
    var text: String = "",
    /**
     * The order of this hyperlink in it's referer page
     * */
    var order: Int = 0,
    /**
     * The url of the referer page
     * */
    override var referer: String? = null,
    /**
     * The url arguments
     * */
    override var args: String? = null,
    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    override var href: String? = null,
    /**
     * The priority
     * */
    override var priority: Int = 0
): UrlAware, Comparable<UrlAware>, StatefulUrl, CompletableFuture<T>() {

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

    override var authToken: String? = null
    override var remoteAddr: String? = null
    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()

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

open class CompletableListenableHyperlink<T>(
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
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    href: String? = null
): UrlAware, Comparable<UrlAware>, ListenableHyperlink,
    CompletableHyperlink<T>(url, text, order, referer, args, href)
{
    override var eventHandler: PulsarEventHandler = DefaultPulsarEventHandler()
}
