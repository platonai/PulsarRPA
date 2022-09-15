package ai.platon.pulsar.crawl.common.url

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.options.OptionUtils
import ai.platon.pulsar.common.urls.*
import ai.platon.pulsar.crawl.DefaultPulsarEvent
import ai.platon.pulsar.crawl.HTMLDocumentHandler
import ai.platon.pulsar.crawl.PulsarEvent
import ai.platon.pulsar.crawl.WebPageHandler
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Document
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer

interface ListenableUrl: UrlAware {
    val event: PulsarEvent
}

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
     * A click url is an url variant, it's the raw url in the html without normalization,
     * for example, an url with a timestamp query parameter added
     * */
    href: String? = null,
    /**
     * The event handler
     * */
    override var event: PulsarEvent = DefaultPulsarEvent(),
): Hyperlink(url, text, order, referer, args, href), ListenableUrl {
    /**
     * A listenable url is not a persistence object because the event handler is not persistent
     * */
    override val isPersistable: Boolean = false
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
        href: String? = null,
        /**
         * The event handler
         * */
        override var event: PulsarEvent = DefaultPulsarEvent()
): StatefulHyperlink(url, text, order, referer, args, href), ListenableUrl {

    override val isPersistable: Boolean = false

    val idleTime get() = Duration.between(modifiedAt, Instant.now())
}

open class ParsableHyperlink(
    /**
     * The url of this hyperlink
     * */
    url: String,
    val onParse: (WebPage, Document) -> Any?
): Hyperlink(url, args = "-parse"), ListenableUrl {

    /**
     * Java compatible constructor
     * */
    constructor(url: String, onParse: BiConsumer<WebPage, Document>):
            this(url, { page, document -> onParse.accept(page, document) })

    override var event: PulsarEvent = DefaultPulsarEvent().also {
        it.loadEvent.onHTMLDocumentParsed.addLast(object: HTMLDocumentHandler() {
            override fun invoke(page: WebPage, document: FeaturedDocument) {
                onParse(page, document.document)
                Unit
            }
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

    override val configuredUrl get() = UrlUtils.mergeUrlArgs(url, args)

    override val isStandard get() = UrlUtils.isValidUrl(url)

    override val toURL get() = URL(url)

    override val toURLOrNull get() = UrlUtils.getURLOrNull(url)

    override val isNil: Boolean get() = url == AppConstants.NIL_PAGE_URL

    /**
     * If this link is persistable
     * */
    override val isPersistable: Boolean = false

    override val label: String get() = OptionUtils.findOption(args, listOf("-l", "-label", "--label")) ?: ""

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
    override var nMaxRetry: Int = 3

    override val deadTime: Instant
        get() {
            val deadTime = OptionUtils.findOption(args, listOf("-deadTime", "--dead-time")) ?: ""
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
    href: String? = null,
    /**
     * The event handler
     * */
    override var event: PulsarEvent = DefaultPulsarEvent()
): UrlAware, Comparable<UrlAware>, ListenableUrl,
    CompletableHyperlink<T>(url, text, order, referer, args, href)

internal class CompleteWebPageHyperlinkHandler(val link: CompletableListenableHyperlink<WebPage>): WebPageHandler() {
    override fun invoke(page: WebPage) {
        link.complete(page)
        link.event.loadEvent.onLoaded.remove(this)
    }
}

/**
 * Create a completable listenable hyperlink
 * */
fun NormUrl.toCompletableListenableHyperlink(): CompletableListenableHyperlink<WebPage> {
    val link = CompletableListenableHyperlink<WebPage>(spec, args = args, href = hrefSpec)

    // make sure every option has its own event handler
    link.event = DefaultPulsarEvent()

    val handler = CompleteWebPageHyperlinkHandler(link)
    link.event.loadEvent.onLoaded.addLast(handler)
    options.event?.let { link.event.combine(it) }

    link.completeOnTimeout(WebPage.NIL, options.pageLoadTimeout.seconds + 1, TimeUnit.SECONDS)

    return link
}
