package ai.platon.pulsar.crawl.parse.html

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_DEFAULT_ENCODING
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.persist.ext.loadEvent
import ai.platon.pulsar.crawl.parse.ParseFilter
import ai.platon.pulsar.crawl.parse.ParseFilters
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.Parser
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Primer HTML parser is a built-in parser while others should be a protocol-plugin.
 * Primer HTML parser uses Jsoup to parse the page content to a HTML document.
 * The parser can also register [ai.platon.pulsar.crawl.parse.ParseFilter]s to extend the system.
 */
class PrimerHtmlParser(
    private val parseFilters: ParseFilters? = null,
    private val conf: ImmutableConfig,
) : Parser {
    companion object {
        val numHtmlParses = AtomicInteger()
        val numHtmlParsed = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(PrimerHtmlParser::class.java)
    private val tracer = logger.takeIf { it.isDebugEnabled }
    private val defaultCharEncoding = conf.get(PARSE_DEFAULT_ENCODING, "utf-8")
    private val primerParser = PrimerParser(conf)
    /**
     * Optimized for html content
     * To parse non-html content, the parser might run into an endless loop,
     * run it in a separate coroutine to protect the process
     * */
    override val timeout = Duration.ZERO

    init {
        logger.info(params.formatAsLine())
    }

    constructor(conf: ImmutableConfig): this(null, conf)

    override fun getParams(): Params {
        return Params.of(
            "className", this.javaClass.simpleName,
            "defaultCharEncoding", defaultCharEncoding,
            "parseFilters", parseFilters
        )
    }

    override fun parse(page: WebPage): ParseResult {
        return try {
            // The base url is set by protocol, it might be different from the page url
            // if the request redirects.
            onWillParseHTMLDocument(page)

            val parseContext = primerParser.parseHTMLDocument(page)

            parseFilters?.filter(parseContext)

            parseContext.document?.let { onHTMLDocumentParsed(page, it) }

            parseContext.parseResult
        } catch (e: MalformedURLException) {
            ParseResult.failed(ParseStatusCodes.FAILED_MALFORMED_URL, e.message)
        } catch (e: Exception) {
            ParseResult.failed(ParseStatusCodes.FAILED_INVALID_FORMAT, e.message)
        }
    }

    /**
     *
     * */
    private fun onWillParseHTMLDocument(page: WebPage) {
        numHtmlParses.incrementAndGet()

        try {
            page.loadEvent?.onWillParseHTMLDocument?.invoke(page)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke onWillParseHTMLDocument | ${page.configuredUrl}", e)
        }
    }

    /**
     *
     * */
    private fun onHTMLDocumentParsed(page: WebPage, document: FeaturedDocument) {
        try {
            page.loadEvent?.onHTMLDocumentParsed?.invoke(page, document)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke onHTMLDocumentParsed | ${page.configuredUrl}", e)
        } finally {
            numHtmlParsed.incrementAndGet()
        }
    }
}
