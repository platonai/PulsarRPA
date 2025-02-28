package ai.platon.pulsar.persist.experimental

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.persist.*
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
    
    var id: Int
    
    var url: String
    var args: String?
    var configuredUrl: String?
    var location: String?
    var baseUrl: String?
    
    var referrer: String?
    var href: String?
    
    var zoneId: ZoneId?
    var batchId: String?
    var fetchPriority: Int
    var fetchCount: Int
    
    var createTime: Instant
    var fetchTime: Instant?
    var prevFetchTime: Instant?
    var prevCrawlTime1: Instant?

    var modifiedTime: Instant?
    var prevModifiedTime: Instant?
    var proxy: String?
    
    var retryDelay: Duration?
    var maxRetries: Int
    var fetchRetries: Int
    
    var metadata: Metadata
    var headers: ProtocolHeaders
    var protocolStatus: ProtocolStatus?
    
    var lastBrowser: BrowserType?
    var generateTime: Instant?
    var openPageCategory: OpenPageCategory
    var pageCategory: PageCategory?

    var htmlIntegrity: HtmlIntegrity
    var activeDOMStatus: ActiveDOMStatus?
    var activeDOMStatTrace: Map<String, ActiveDOMStat>?

    var encoding: String?
    var contentType: String?
    var contentLength: Long
    var aveContentLength: Long
    var persistedContentLength: Long
    var lastContentLength: Long

    var persistContent: ByteBuffer?
    var content: ByteBuffer?
    var contentAsBytes: ByteArray?
    var contentAsString: String?
    var contentAsSaxInputSource: InputSource?
    var contentAsInputStream: ByteArrayInputStream?
    
    var parseStatus: ParseStatus?
    var crawlStatus: CrawlStatus
    
    var pageTitle: String?
    
    var liveLinks: Map<CharSequence, GHypeLink>?
    var vividLinks: Map<CharSequence, CharSequence>?
    var deadLinks: List<CharSequence>?
    var links: List<CharSequence>?
    var estimatedLinkCount: Int
    var anchor: CharSequence?
    var anchorOrder: Int
    var fetchedLinkCount: Int
    
    var pageModelUpdateTime: Instant?
    var pageModel: PageModel?
}
