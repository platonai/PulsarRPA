package ai.platon.pulsar.common.url

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.AppConstants
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * The UrlAware interface.
 * */
interface UrlAware: Comparable<UrlAware> {
    var url: String
    var args: String?
    var referer: String?
    val configuredUrl: String
    val isNil: Boolean
}

/**
 * The StatefulUrl interface. A StatefulUrl is an UrlAware and has status.
 * */
interface StatefulUrl: UrlAware, Comparable<UrlAware> {
    var status: Int
    var modifiedAt: Instant
    val createdAt: Instant
}

abstract class AbstractUrl(
        override var url: String,
        override var args: String? = null,
        override var referer: String? = null
): UrlAware {

    override val configuredUrl get() = if (args != null) "$url $args" else url

    override val isNil: Boolean get() = url == AppConstants.NIL_PAGE_URL

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

abstract class AbstractStatefulUrl(
        url: String,
        args: String? = null,
        referer: String? = null
): AbstractUrl(url, args, referer), StatefulUrl {
    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()
}

open class PlainUrl(
        url: String,
        args: String? = null,
        referer: String? = null
): AbstractUrl(url, args, referer)

data class HyperlinkDatum(
        val url: String,
        /**
         * A hyperlink should have a text, so the default value is an empty string
         * */
        val text: String = "",
        val order: Int = 0,
        /**
         * A hyperlink might have a referer, so the default value is null
         * */
        val referer: String? = null,
        /**
         * A programmer might give a argument to a hyperlink, so the default value is null
         * */
        val args: String? = null
)

/**
 * A [hyperlink](https://en.wikipedia.org/wiki/Hyperlink), or simply a link, is a reference to data that the user can
 * follow by clicking or tapping.
 *
 * A hyperlink points to a whole document or to a specific element within a document.
 * Hypertext is text with hyperlinks. The text that is linked from is called anchor text.
 *
 * The [anchor text](https://en.wikipedia.org/wiki/Anchor_text), link label or link text is the visible,
 * clickable text in an HTML hyperlink
 * */
open class Hyperlink(
        /**
         * The url of this hyperlink
         * */
        url: String,
        /**
         * The anchor text of this hyperlink
         * */
        val text: String = "",
        /**
         * The order of this hyperlink in it's referer page
         * */
        val order: Int = 0,
        /**
         * The url of the referer page
         * */
        referer: String? = null,
        /**
         * The url arguments
         * */
        args: String? = null
): AbstractUrl(url, args = args, referer = referer) {
    constructor(url: UrlAware): this(url.url, "", 0, url.referer, url.args)
    constructor(url: Hyperlink): this(url.url, url.text, url.order, url.referer, url.args)
    constructor(url: HyperlinkDatum): this(url.url, url.text, url.order, url.referer, url.args)

    fun data() = HyperlinkDatum(url, text, order, referer, args)
}

data class LabeledHyperlinkDatum(
        /**
         * The label
         * */
        var label: String,
        /**
         * The url of this hyperlink
         * */
        val url: String,
        /**
         * A hyperlink should have a text, so the default value is an empty string
         * */
        val text: String = "",
        /**
         * The order of this hyperlink in it's referer page
         * */
        val order: Int = 0,
        /**
         * A hyperlink might have a referer, so the default value is null
         * */
        val referer: String? = null,
        /**
         * The depth of the link, it should be [the depth of the referer] + 1
         * */
        val depth: Int = 0,
        /**
         * A programmer might give a argument to a hyperlink, so the default value is null
         * */
        val args: String? = null
)

open class LabeledHyperlink(
        /**
         * The label
         * */
        var label: String,
        /**
         * The url of this hyperlink
         * */
        url: String,
        /**
         * A hyperlink should have a text, so the default value is an empty string
         * */
        val text: String = "",
        /**
         * The order of this hyperlink in it's referer page
         * */
        val order: Int = 0,
        /**
         * A hyperlink might have a referer, so the default value is null
         * */
        referer: String? = null,
        /**
         * The depth of the link, it should be [the depth of the referer] + 1
         * */
        val depth: Int = 0
): AbstractUrl(url) {
    fun data() = LabeledHyperlinkDatum(label, url, text, order, referer, depth, args)
}

open class StatefulHyperlink(
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
        referer: String? = null
): Hyperlink(url, text, order, referer), StatefulUrl {
    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()
}

val StatefulHyperlink.isCreated get() = this.status == ResourceStatus.SC_CREATED
val StatefulHyperlink.isAccepted get() = this.status == ResourceStatus.SC_ACCEPTED
val StatefulHyperlink.isProcessing get() = this.status == ResourceStatus.SC_PROCESSING
val StatefulHyperlink.isFinished get() = !isCreated && !isAccepted && !isProcessing

/**
 * A (fat link)[https://en.wikipedia.org/wiki/Hyperlink#Fat_link] (also known as a "one-to-many" link, an "extended link"
 * or a "multi-tailed link") is a hyperlink which leads to multiple endpoints; the link is a multivalued function.
 * */
open class FatLink(
        url: String,
        var tailLinks: List<StatefulHyperlink>,
        text: String = "",
        order: Int = 0
): Hyperlink(url, text, order) {
    val size get() = tailLinks.size
    val isEmpty get() = size == 0
    val isNotEmpty get() = !isEmpty

    override fun toString() = "$size | $url"
}

open class StatefulFatLink(
        url: String,
        tailLinks: List<StatefulHyperlink>,
        text: String = "",
        order: Int = 0
): FatLink(url, tailLinks, text, order), StatefulUrl {

    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()

    override fun toString() = "$status $createdAt $modifiedAt ${super.toString()}"
}

open class LabeledStatefulFatLink(
        val label: String,
        url: String,
        tailLinks: List<StatefulHyperlink>,
        text: String = "",
        order: Int = 0
): StatefulFatLink(url, tailLinks, text, order), StatefulUrl {
    override fun toString() = "$label ${super.toString()}"
}

class CrawlableFatLink(
        label: String,
        url: String,
        tailLinks: List<StatefulHyperlink>,
        text: String = "",
        order: Int = 0
): LabeledStatefulFatLink(label, url, tailLinks, text, order) {

    private val log = LoggerFactory.getLogger(CrawlableFatLink::class.java)

    val numFinished = AtomicInteger()
    var aborted = false
    var authToken: String? = null
    var remoteAddr: String? = null

    val numActive get() = size - numFinished.get()
    val isAborted get() = aborted
    val isFinished get() = numFinished.get() >= size
    val idleTime get() = Duration.between(modifiedAt, Instant.now())

    fun abort() {
        aborted = true
    }

    fun finish(url: StatefulHyperlink, status: Int = ResourceStatus.SC_OK): Boolean {
        aborted = false

        if (log.isDebugEnabled) {
            log.debug("Try to finish stateful hyperlink, ({}) \n{} \n{}",
                    if (url in tailLinks) "found" else "not found", url, this)
        }

        if (tailLinks.none { url.url == it.url }) {
            return false
        }

        require(url.referer == this.url)
        url.modifiedAt = Instant.now()
        url.status = status
        modifiedAt = Instant.now()
        numFinished.incrementAndGet()
        if (isFinished) {
            log.info("Crawlable fat link is finished | {}", this)
            this.status = ResourceStatus.SC_OK
        }

        return true
    }

    override fun toString() = "$numFinished/${tailLinks.size} ${super.toString()}"
}
