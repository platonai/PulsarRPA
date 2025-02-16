package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.ResourceStatus
import java.time.Instant

open class StatefulHyperlink(
    /**
     * The url specification of the hyperlink, it is usually normalized, and can contain load arguments.
     * */
    url: String,
    /**
     * The anchor text is not always available and is not a required field.
     * It can easily be filled by args by mistake, so we require you to fill this field in the current version.
     * We plan to move this field to a later position in future versions.
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
    depth: Int = 0
) : Hyperlink(url, text, order, referrer, args, href, priority, lang, country, district, nMaxRetry, depth),
    StatefulUrl {
    override var authToken: String? = null
    override var remoteAddr: String? = null
    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()
    
    val isCreated get() = this.status == ResourceStatus.SC_CREATED
    val isAccepted get() = this.status == ResourceStatus.SC_ACCEPTED
    val isProcessing get() = this.status == ResourceStatus.SC_PROCESSING
    val isFinished get() = !isCreated && !isAccepted && !isProcessing
}