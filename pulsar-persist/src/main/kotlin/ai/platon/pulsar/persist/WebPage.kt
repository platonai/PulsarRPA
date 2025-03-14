package ai.platon.pulsar.persist

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.FetchMode
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

    var href: String?

    val isNil: Boolean

    val isNotNil: Boolean

    val isInternal: Boolean

    val isNotInternal: Boolean

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

    var args: String

    val configuredUrl: String

    var retryDelay: Duration

    var maxRetries: Int

    var zoneId: ZoneId

    var distance: Int

    var fetchMode: FetchMode

    var lastBrowser: BrowserType

    var isResource: Boolean

    var htmlIntegrity: HtmlIntegrity

    var fetchPriority: Int

    var createTime: Instant

    var generateTime: Instant

    var fetchCount: Int

    var baseUrl: String

    var location: String

    var fetchTime: Instant

    var fetchRetries: Int

    var prevFetchTime: Instant

    var prevCrawlTime1: Instant

    var fetchInterval: Duration

    var modifiedTime: Instant

    var prevModifiedTime: Instant

    var protocolStatus: ProtocolStatus

    val headers: ProtocolHeaders

    var pageCategory: PageCategory

    val openPageCategory: OpenPageCategory

    var encoding: String?

    var contentType: String

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

    fun setLazyFieldLoader(lazyFieldLoader: Function<String, GWebPage>)
}
