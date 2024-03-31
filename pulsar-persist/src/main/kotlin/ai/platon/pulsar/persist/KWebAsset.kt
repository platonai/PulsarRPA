package ai.platon.pulsar.persist

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.VolatileConfig
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

interface KWebAsset {
    
    val id: Int
    
    val url: String
    val args: String?
    val configuredUrl: String?
    val location: String?
    val baseUrl: String?
    
    val referrer: String?
    val href: String?
    
    val variables: Variables
    val conf: VolatileConfig?
    
    val zoneId: ZoneId?
    val batchId: String?
    val fetchPriority: Int
    val fetchCount: Int
    
    val createTime: Instant
    val fetchTime: Instant?
    val prevFetchTime: Instant?
    val prevCrawlTime1: Instant?
    val fetchInterval: Duration?
    
    val modifiedTime: Instant?
    val prevModifiedTime: Instant?
    val proxy: String?
    
    val retryDelay: Duration?
    val maxRetries: Int
    val fetchRetries: Int
    
    val metadata: Metadata
    val headers: ProtocolHeaders
    val protocolStatus: ProtocolStatus?
    
    val lastBrowser: BrowserType?
    val generateTime: Instant?
    val openPageCategory: OpenPageCategory
    val pageCategory: PageCategory?
    
    val htmlIntegrity: HtmlIntegrity
    val activeDOMStatus: ActiveDOMStatus?
    val activeDOMStatTrace: Map<String, ActiveDOMStat>?
    
    val encoding: String?
    val contentType: String?
    val contentLength: Long
    
    val persistContent: ByteBuffer?
    val content: ByteBuffer?
    val contentAsBytes: ByteArray?
    val contentAsString: String?
    val contentAsSaxInputSource: InputSource?
    val contentAsInputStream: ByteArrayInputStream?
    
    val aveContentLength: Long
    val persistedContentLength: Long
    val lastContentLength: Long
    
    val parseStatus: ParseStatus?
    val crawlStatus: CrawlStatus
    
    val pageTitle: String?
    
    val liveLinks: Map<CharSequence, GHypeLink>?
    val vividLinks: Map<CharSequence, CharSequence>?
    val deadLinks: List<CharSequence>?
    val links: List<CharSequence>?
    val estimatedLinkCount: Int
    val anchor: CharSequence?
    val anchorOrder: Int
    val fetchedLinkCount: Int
    
    val pageModelUpdateTime: Instant?
    val pageModel: PageModel?
}
