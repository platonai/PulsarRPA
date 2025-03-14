package ai.platon.pulsar.persist.experimental

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.persist.Metadata
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.ProtocolHeaders
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.metadata.OpenPageCategory
import ai.platon.pulsar.persist.metadata.PageCategory
import ai.platon.pulsar.persist.model.ActiveDOMStat
import ai.platon.pulsar.persist.model.ActiveDOMStatus
import ai.platon.pulsar.persist.model.PageModel
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class NilWebAsset : KWebAsset {
    override val id: Int = 0
    override val url: String = ""
    override val location: String? = null
    override val baseUrl: String? = null
    override val args: String? = null
    override val configuredUrl: String? = null
    override val referrer: String? = null
    override val href: String? = null
    override val metadata: Metadata = Metadata.box(mapOf())
    override val headers: ProtocolHeaders = ProtocolHeaders.box(mapOf())
    override val createTime: Instant = Instant.EPOCH
    override val zoneId: ZoneId? = null
    override val batchId: String? = null
    override val fetchPriority: Int = 0
    override val fetchCount: Int = 0
    override val fetchTime: Instant? = null
    override val prevFetchTime: Instant? = null
    override val prevCrawlTime1: Instant? = null
    override val fetchInterval: Duration? = null
    override val protocolStatus: ProtocolStatus? = null
    override val fetchRetries: Int = 0
    override val modifiedTime: Instant? = null
    override val prevModifiedTime: Instant? = null
    override val pageCategory: PageCategory? = null
    override val proxy: String? = null
    override val activeDOMStatus: ActiveDOMStatus? = null
    override val activeDOMStatTrace: Map<String, ActiveDOMStat>? = null
    override val retryDelay: Duration? = null
    override val maxRetries: Int = 3
    override val fetchedLinkCount: Int = 0
    override val lastBrowser: BrowserType? = null
    override val generateTime: Instant? = null
    override val openPageCategory: OpenPageCategory = OpenPageCategory(PageCategory.UNKNOWN)
    override val encoding: String? = null
    override val htmlIntegrity: HtmlIntegrity = HtmlIntegrity.OK
    override val pageTitle: String? = null
    override val contentType: String? = null
    override val contentLength: Long = 0
    override val aveContentLength: Long = 0
    override val persistedContentLength: Long = 0
    override val lastContentLength: Long = 0
    override val content: ByteBuffer? = null
    override val persistContent: ByteBuffer? = null
    override val contentAsBytes: ByteArray? = null
    override val contentAsString: String? = null
    override val contentAsSaxInputSource: InputSource? = null
    override val contentAsInputStream: ByteArrayInputStream? = null
    override val parseStatus: ParseStatus? = null
    override val liveLinks: Map<CharSequence, GHypeLink>? = null
    override val vividLinks: Map<CharSequence, CharSequence>? = null
    override val deadLinks: List<CharSequence>? = null
    override val links: List<CharSequence>? = null
    override val estimatedLinkCount: Int = 0
    override val anchor: CharSequence? = null
    override val anchorOrder: Int = 0
    override val pageModelUpdateTime: Instant? = null
    override val pageModel: PageModel? = null
}
