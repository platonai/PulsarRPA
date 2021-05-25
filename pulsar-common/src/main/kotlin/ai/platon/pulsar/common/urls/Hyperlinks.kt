package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.options.findOption
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.Instant

interface DegenerateUrl

/**
 * The UrlAware interface.
 * */
interface UrlAware {
    /**
     * The url, it can be configured or not
     * */
    var url: String

    /**
     * The url args
     * */
    var args: String?

    /**
     * The referer(or referrer)
     * */
    var referer: String?

    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    var href: String?

    /**
     * The configured url, always be "$url $args"
     * */
    val configuredUrl: String

    /**
     * If this is a Nil url who's url is AppConstants.NIL_PAGE_URL
     * */
    val isNil: Boolean

    /**
     * If this link is persistable
     * */
    val isPersistable: Boolean

    /**
     * The url label, it should be in args
     * */
    val label: String

    /**
     * The url label, it should be in args
     * */
    val deadTime: Instant

    /**
     * Required website language
     * */
    val lang: String

    /**
     * Required website country
     * */
    val country: String

    /**
     * Required website district
     * */
    val district: String

    /**
     * The maximum retry times
     * */
    val maxRetry: Int
}

interface ComparableUrlAware : UrlAware, Comparable<UrlAware>

/**
 * The StatefulUrl interface. A StatefulUrl is an UrlAware and has status.
 * */
interface StatefulUrl : ComparableUrlAware {
    var authToken: String?
    var remoteAddr: String?
    var status: Int
    var modifiedAt: Instant
    val createdAt: Instant
}

abstract class AbstractUrl(
    override var url: String,
    override var args: String? = null,
    override var referer: String? = null,
    override var href: String? = null
) : UrlAware, ComparableUrlAware {

    override val configuredUrl get() = if (args != null) "$url $args" else url

    override val isNil: Boolean get() = url == AppConstants.NIL_PAGE_URL

    /**
     * If this link is persistable
     * */
    override val isPersistable: Boolean = true

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

    override fun compareTo(other: UrlAware): Int {
        return url.compareTo(other.url)
    }

    override fun hashCode() = url.hashCode()

//    override fun compareTo(other: UrlAware) = url.compareTo(other.url)

    override fun toString() = url
}

abstract class AbstractStatefulUrl(
    url: String,
    args: String? = null,
    referer: String? = null
) : AbstractUrl(url, args, referer), StatefulUrl {
    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()
}

open class PlainUrl(
    url: String,
    args: String? = null,
    referer: String? = null
) : AbstractUrl(url, args, referer)

data class HyperlinkDatum(
    val url: String,
    /**
     * A hyperlink should have a text, so the default value is an empty string
     * */
    val text: String = "",
    /**
     * The link order, e.g., in the referer page
     * */
    val order: Int = 0,
    /**
     * A hyperlink might have a referer, so the default value is null
     * */
    val referer: String? = null,
    /**
     * A programmer might give a argument to a hyperlink, so the default value is null
     * */
    val args: String? = null,
    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    var href: String? = null,
    /**
     * If this link is persistable
     * */
    val isPersistable: Boolean = false,
    /**
     * The depth
     * */
    val depth: Int = 0
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
    var text: String = "",
    /**
     * The order of this hyperlink in it's referer page
     * */
    var order: Int = 0,
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
) : AbstractUrl(url, args, referer, href) {
    var depth: Int = 0

    constructor(url: UrlAware) : this(url.url, "", 0, url.referer, url.args, href = url.href)
    constructor(url: Hyperlink) : this(url.url, url.text, url.order, url.referer, url.args, href = url.href)
    constructor(url: HyperlinkDatum) : this(url.url, url.text, url.order, url.referer, url.args, href = url.href)

    fun data() = HyperlinkDatum(url, text, order, referer = referer, args = args, href = href, true, 0)
}

open class LabeledHyperlink(
    /**
     * The url of this hyperlink
     * */
    override val label: String,
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
) : Hyperlink(url, text, order, referer, args, href)

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
) : Hyperlink(url, text, order, referer, args, href), StatefulUrl {
    override var authToken: String? = null
    override var remoteAddr: String? = null
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
     * The tail links
     * */
    var tailLinks: List<StatefulHyperlink>
) : Hyperlink(url, text, order, referer, args, href) {
    val size get() = tailLinks.size
    val isEmpty get() = size == 0
    val isNotEmpty get() = !isEmpty

    override fun toString() = "$size | $url"
}

open class StatefulFatLink(
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
     * The tail links
     * */
    tailLinks: List<StatefulHyperlink>
) : FatLink(url, text, order, referer, args, href, tailLinks), StatefulUrl {
    override var authToken: String? = null
    override var remoteAddr: String? = null
    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()

    override fun toString() = "$status $createdAt $modifiedAt ${super.toString()}"
}

open class CrawlableFatLink(
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
     * The tail links
     * */
    tailLinks: List<StatefulHyperlink> = listOf()
) : StatefulFatLink(url, text, order, referer, args, href, tailLinks) {

    private val log = LoggerFactory.getLogger(CrawlableFatLink::class.java)

    @Volatile
    var finishedTailLinkCount = 0
    var aborted = false

    val numActive get() = size - finishedTailLinkCount
    val isAborted get() = aborted
    val isFinished get() = finishedTailLinkCount >= size
    val idleTime get() = Duration.between(modifiedAt, Instant.now())

    fun abort() {
        aborted = true
    }

    fun finish(url: StatefulHyperlink, status: Int = ResourceStatus.SC_OK): Boolean {
        aborted = false

        if (log.isDebugEnabled) {
            log.debug(
                "Try to finish stateful hyperlink, ({}) \n{} \n{}",
                if (url in tailLinks) "found" else "not found", url, this
            )
        }

        if (tailLinks.none { url.url == it.url }) {
            return false
        }

        require(url.referer == this.url)
        url.modifiedAt = Instant.now()
        url.status = status
        modifiedAt = Instant.now()
        ++finishedTailLinkCount
        if (isFinished) {
            log.info("Crawlable fat link is finished | {}", this)
            this.status = ResourceStatus.SC_OK
        }

        return true
    }

    override fun toString() = "$finishedTailLinkCount/${tailLinks.size} ${super.toString()}"
}

object Hyperlinks {

    fun toHyperlink(url: UrlAware): Hyperlink {
        return if (url is Hyperlink) url
        else Hyperlink(url.url, args = url.args, referer = url.referer, href = url.href)
    }
}
