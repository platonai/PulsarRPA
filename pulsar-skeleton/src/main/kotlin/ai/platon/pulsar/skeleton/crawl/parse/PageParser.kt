
package ai.platon.pulsar.skeleton.crawl.parse

import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.skeleton.common.message.MiscMessageWriter
import ai.platon.pulsar.skeleton.common.metrics.MetricsSystem
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.skeleton.crawl.common.LazyConfigurable
import ai.platon.pulsar.skeleton.crawl.common.InternalURLUtil
import ai.platon.pulsar.skeleton.crawl.filter.CrawlFilters
import ai.platon.pulsar.skeleton.crawl.filter.SCOPE_FETCH
import ai.platon.pulsar.skeleton.signature.Signature
import ai.platon.pulsar.skeleton.signature.TextMD5Signature
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageExt
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.Mark
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import ai.platon.pulsar.skeleton.common.persist.ext.loadEventHandlers
import ai.platon.pulsar.skeleton.crawl.GlobalEventHandlers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class PageParser(
    val parserFactory: ParserFactory,
    override var conf: ImmutableConfig,
    val crawlFilters: CrawlFilters = CrawlFilters(conf),
    val signature: Signature = TextMD5Signature(),
    val messageWriter: MiscMessageWriter? = null
) : Parameterized, LazyConfigurable, AutoCloseable {

    enum class Counter { notFetched, alreadyParsed, truncated, notParsed, parseSuccess, parseFailed }
    init { MetricsSystem.reg.register(Counter::class.java) }

    private val parseCount = AtomicInteger()

    val unparsableTypes = ConcurrentSkipListSet<CharSequence>()

    constructor(parserFactory: ParserFactory, conf: ImmutableConfig) : this(
        parserFactory,
        conf,
        CrawlFilters(conf),
        TextMD5Signature(),
        MiscMessageWriter()
    )

    /**
     * @param conf The configuration
     */
    constructor(conf: ImmutableConfig) : this(ParserFactory(conf), conf)

    init {
        params.withLogger(logger).info(true)
    }

    override fun configure(conf1: ImmutableConfig) {
    }

    /**
     * Parses given web page and stores parsed content within page. Puts a meta-redirect to outlinks.
     *
     * @param page The web page
     * @return ParseResult
     * A non-null ParseResult contains the main result and status of this parse
     */
    fun parse(page: WebPage): ParseResult {
        parseCount.incrementAndGet()

        try {
            val parseResult = doParse(page)

            if (parseResult.isParsed) {
                page.parseStatus = parseResult

                if (parseResult.isRedirect) {
                    processRedirect(page, parseResult)
                } else if (parseResult.isSuccess) {
                    processSuccess(page, parseResult)
                }
                updateCounters(parseResult)

                if (parseResult.isSuccess) {
                    page.marks.putIfNotNull(Mark.PARSE, page.marks[Mark.FETCH])
                }
            }

            return parseResult
        } catch (e: Throwable) {
            logger.error(e.stringify())
        }

        return ParseResult()
    }

    private fun doParse(page: WebPage): ParseResult {
        if (page.isInternal) {
            return ParseResult().apply { flowStatus = FlowState.BREAK }
        }

        val url = page.url
        if (isTruncated(page)) {
            return ParseResult.failed(ParseStatus.FAILED_TRUNCATED, url)
        }

        return try {
            onWillParse(page)
            applyParsers(page)
        } catch (e: ParserNotFound) {
            unparsableTypes.add(page.contentType)
            logger.warn("No parser found for <" + page.contentType + ">\n" + e.message)
            return ParseResult.failed(ParseStatus.FAILED_NO_PARSER, page.contentType)
        } catch (e: Throwable) {
            logger.warn("Failed to parse | ${page.configuredUrl}", e)
            return ParseResult.failed(e)
        } finally {
            onParsed(page)
        }
    }

    private fun onWillParse(page: WebPage) {
        try {
            // The more specific handlers has the opportunity to override the result of more general handlers.
            page.loadEventHandlers?.onWillParse?.invoke(page)
            GlobalEventHandlers.pageEventHandlers?.loadEventHandlers?.onWillParse?.invoke(page)
        } catch (e: Throwable) {
            logger.warn("[onWillParse]", e)
        }
    }

    private fun onParsed(page: WebPage) {
        try {
            GlobalEventHandlers.pageEventHandlers?.loadEventHandlers?.onParsed?.invoke(page)
            // The more specific handlers has the opportunity to override the result of more general handlers.
            page.loadEventHandlers?.onParsed?.invoke(page)
        } catch (e: Throwable) {
            logger.warn("[onParsed]", e)
        }
    }

    /**
     * @throws ParseException If there is an error parsing.
     */
    @Throws(ParseException::class)
    private fun applyParsers(page: WebPage): ParseResult {
        if (page.isInternal) {
            return ParseResult().apply { flowStatus = FlowState.BREAK }
        }

        var parseResult = ParseResult()
        val parsers = parserFactory.getParsers(page.contentType, page.url)

        for (parser in parsers) {
            val millis = measureTimeMillis {
                // Optimized for html content
                // To parse non-html content, the parser might run into an endless loop,
                // run it in a separate coroutine to protect the process
                parseResult = takeIf { parser.timeout.seconds > 0 }?.runParser(parser, page)?:parser.parse(page)
            }
            parseResult.parsers.add(parser::class)

            val m = page.pageModel
            if (logger.isDebugEnabled && millis > 10_000 && m != null) {
                logger.debug("It takes {} to parse {}/{}/{} fields | {}", Duration.ofMillis(millis).readable(),
                        m.numNonBlankFields, m.numNonNullFields, m.numFields, page.url)
            }

            // Found a suitable parser and successfully parsed
            if (parseResult.shouldBreak) {
                break
            }
        }

        return parseResult
    }

    /**
     * TODO: might be optimized
     * */
    private fun runParser(p: Parser, page: WebPage): ParseResult {
        return runBlocking {
            withTimeout(p.timeout.toMillis()) {
                val deferred = async { p.parse(page) }
                deferred.await()
            }
        }
    }

    /**
     * Note: signature is not useful for pages change rapidly
     * */
    private fun processSuccess(page: WebPage, parseResult: ParseResult) {
        val prevSig = page.signature
        if (prevSig != null) {
            page.prevSignature = prevSig
        }
        signature.calculate(page)?.let { page.setSignature(it) }

        if (parseResult.hypeLinks.isNotEmpty()) {
            processLinks(page, parseResult.hypeLinks)
        }
    }

    /**
     * Process redirect when the page is fetched with native http protocol rather than a browser
     * */
    private fun processRedirect(page: WebPage, parseStatus: ParseStatus) {
        val refreshHref = parseStatus.getArgOrElse(ParseStatus.REFRESH_HREF, "")
        val newUrl = crawlFilters.normalizeToNull(refreshHref, SCOPE_FETCH) ?: return

        page.addLiveLink(HyperlinkPersistable(newUrl))
        page.metadata[Name.REDIRECT_DISCOVERED] = AppConstants.YES_STRING
        if (newUrl == page.url) {
            val refreshTime = parseStatus.getArgOrElse(ParseStatus.REFRESH_TIME, "0").toInt()
            val reprUrl = InternalURLUtil.chooseRepr(page.url, newUrl, refreshTime < AppConstants.PERM_REFRESH_TIME)
            page.reprUrl = reprUrl
        }
    }

    private fun processLinks(page: WebPage, unfilteredLinks: MutableSet<HyperlinkPersistable>) {
        // Collect links
        // TODO : check the no-follow html tag directive
        val follow = (!page.metadata.contains(Name.NO_FOLLOW)
                || page.isSeed
                || page.hasMark(Mark.INJECT)
                || page.metadata.contains(Name.FORCE_FOLLOW)
                || page.variables.contains(Name.FORCE_FOLLOW.name))
        if (follow) {
            val pageExt = WebPageExt(page)
            // val hypeLinks = filterLinks(page, unfilteredLinks)
            // TODO: too many filters, hard to debug, move all filters to a single filter, or just do it in ParserFilter
            val hypeLinks = unfilteredLinks
            logger.takeIf { it.isTraceEnabled }?.trace("Find {}/{} live links", hypeLinks.size, unfilteredLinks.size)
            page.setLiveLinks(hypeLinks)
            pageExt.addHyperlinks(hypeLinks)
        }
    }

    // 0 : "notparsed", 1 : "success", 2 : "failed"
    private fun updateCounters(parseStatus: ParseStatus) {
        var counter: Counter? = null
        when (parseStatus.majorCode) {
            ParseStatusCodes.FAILED -> counter = Counter.parseFailed
            else -> {
            }
        }

        if (counter != null) {
            MetricsSystem.reg.enumCounterRegistry.inc(counter)
        }
    }

    override fun close() {
    }

    companion object {
        val logger = LoggerFactory.getLogger(PageParser::class.java)

        /**
         * Checks if the page's content is truncated.
         *
         * @param page The web page
         * @return If the page is truncated `true`. When it is not, or when
         * it could be determined, `false`.
         */
        fun isTruncated(page: WebPage): Boolean {
            if (page.fetchMode == FetchMode.BROWSER) {
                val hi = page.htmlIntegrity
                return when {
                    hi.isOK -> false
                    hi.isOther -> page.contentLength < 20_000
                    else -> true
                }
            }

            if (page.fetchMode != FetchMode.NATIVE) {
                return false
            }

            val url = page.url
            val inHeaderSize = page.headers.contentLength
            if (inHeaderSize < 0) {
                logger.trace("HttpHeaders.CONTENT_LENGTH is not available | $url")
                return false
            }

            val content = page.content
            if (content == null) {
                logger.debug("Page content is null, url: $url")
                return false
            }

            val actualSize = content.limit()
            return inHeaderSize > actualSize
        }
    }
}
