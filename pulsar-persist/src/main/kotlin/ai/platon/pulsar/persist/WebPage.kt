package ai.platon.pulsar.persist

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.Mark
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
import java.util.function.Function

interface WebPage : Comparable<WebPage> {
    val id: Int

    val url: String

    val key: String

    val reversedUrl: String

    var href: String?

    val isNil: Boolean

    val isNotNil: Boolean

    val isInternal: Boolean

    val isNotInternal: Boolean

    @Deprecated(
        """Use the method in concrete type of the page
      """
    )
    fun unbox(): GWebPage

    @Deprecated(
        """Use the method in concrete type of the page
      """
    )
    fun unsafeSetGPage(page: GWebPage)

    fun unsafeCloneGPage(page: WebPage)

    val variables: Variables

    fun hasVar(name: String): Boolean

    fun getVar(name: String): Any?

    fun getVar(clazz: Class<*>): Any?

    fun removeVar(name: String): Any?

    fun setVar(name: String, value: Any)

    fun getBean(clazz: Class<*>): Any

    fun getBeanOrNull(clazz: Class<*>): Any?

    fun <T> putBean(bean: T)

    fun data(name: String): Any?

    fun data(name: String, value: Any?)

    var pageDatum: PageDatum?

    var isCached: Boolean

    var isLoaded: Boolean

    var isFetched: Boolean

    var isCanceled: Boolean

    val isContentUpdated: Boolean

    var conf: VolatileConfig

    val metadata: Metadata

    val marks: CrawlMarks

    fun hasMark(mark: Mark): Boolean

    var args: String
    
    var retryDelay: Duration

    fun setLazyFieldLoader(lazyFieldLoader: Function<String, GWebPage>)

    var maxRetries: Int

    val configuredUrl: String

    var fetchedLinkCount: Int

    var zoneId: ZoneId

    var batchId: String?

    fun markSeed()

    fun unmarkSeed()

    val isSeed: Boolean

    var distance: Int

    var fetchMode: FetchMode

    var lastBrowser: BrowserType

    var isResource: Boolean

    var htmlIntegrity: HtmlIntegrity

    var fetchPriority: Int

    var createTime: Instant

    var generateTime: Instant

    var fetchCount: Int

    fun updateFetchCount()

    var crawlStatus: CrawlStatus

    fun setCrawlStatus(value: Int)

    var baseUrl: String

    var location: String

    var fetchTime: Instant

    var prevFetchTime: Instant

    var prevCrawlTime1: Instant

    var fetchInterval: Duration

    var protocolStatus: ProtocolStatus

    val headers: ProtocolHeaders

    var reprUrl: String

    var fetchRetries: Int

    var modifiedTime: Instant

    var prevModifiedTime: Instant

    var pageCategory: PageCategory

    val openPageCategory: OpenPageCategory

    fun setPageCategory(pageCategory: OpenPageCategory)

    var encoding: String?

    val content: ByteBuffer?

    var tmpContent: ByteBuffer?

    val persistContent: ByteBuffer?
    
    val contentAsBytes: ByteArray

    val contentAsString: String

    val contentAsInputStream: ByteArrayInputStream

    val contentAsSaxInputSource: InputSource?

    fun setStringContent(value: String?)

    fun setByteArrayContent(value: ByteArray?)

    fun setByteBufferContent(value: ByteBuffer?)

    fun clearPersistContent()

    val contentLength: Long

    var originalContentLength: Long

    var persistedContentLength: Long

    val lastContentLength: Long

    val aveContentLength: Long

    var contentType: String

    var prevSignature: ByteBuffer?

    val prevSignatureAsString: String

    val signature: ByteBuffer?

    val signatureAsString: String

    var proxy: String?

    var activeDOMStatus: ActiveDOMStatus?

    var activeDOMStatTrace: Map<String, ActiveDOMStat?>

    var pageTitle: String?
    
    var contentTitle: String?

    var pageText: String?

    val contentText: String?

    var parseStatus: ParseStatus

    var links: MutableList<CharSequence>

    val vividLinks: MutableMap<CharSequence, CharSequence>

    val inlinks: MutableMap<CharSequence, CharSequence>
    
    var anchor: CharSequence?

    var referrer: String?

    var pageModelUpdateTime: Instant?

    val pageModel: PageModel?

    fun ensurePageModel(): PageModel
}
