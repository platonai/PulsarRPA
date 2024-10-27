package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.ResourceStatus
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

/**
 * A [crawlable fat link](https://en.wikipedia.org/wiki/Hyperlink#Fat_link) is a hyperlink which leads to multiple endpoints;
 * the link is a multivalued function. It is used in web crawling to represent a page with multiple links.
 * */
open class CrawlableFatLink(
    /**
     * The url specification of the hyperlink, it is usually normalized, and can contain load arguments.
     * */
    url: String,
    /**
     * The anchor text
     * */
    text: String = "",
    /**
     * The order of this hyperlink in it referrer page
     * */
    order: Int = 0,
    /**
     * The url of the referrer page
     * */
    referrer: String? = null,
    /**
     * The additional url arguments
     * */
    args: String? = null,
    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    href: String? = null,
    /**
     * The priority of this hyperlink
     * */
    priority: Int = 0,
    /**
     * The language of this hyperlink
     * */
    lang: String = "*",
    /**
     * The country of this hyperlink
     * */
    country: String = "*",
    /**
     * The district of this hyperlink
     * */
    district: String = "*",
    /**
     * The maximum number of retries
     * */
    nMaxRetry: Int = 3,
    /**
     * The depth of this hyperlink
     * */
    depth: Int = 0,
    /**
     * The tail links
     * */
    tailLinks: List<StatefulHyperlink> = listOf(),
) : StatefulFatLink(url, text, order, referrer, args, href, priority, lang, country, district, nMaxRetry, depth, tailLinks) {

    private val log = LoggerFactory.getLogger(CrawlableFatLink::class.java)

    @Volatile
    var finishedTailLinkCount = 0
    var aborted = false
    /**
     * The number of active tail links
     * */
    val numActive get() = size - finishedTailLinkCount
    /**
     * Whether the crawlable fat link is aborted
     * */
    val isAborted get() = aborted
    /**
     * Whether the crawlable fat link is finished
     * */
    val isFinished get() = finishedTailLinkCount >= size
    /**
     * The idle time of the crawlable fat link
     * */
    val idleTime get() = Duration.between(modifiedAt, Instant.now())
    /**
     * Abort the crawlable fat link
     * */
    fun abort() {
        aborted = true
    }
    /**
     * Finish a stateful hyperlink
     * */
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
        
        require(url.referrer == this.url)
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