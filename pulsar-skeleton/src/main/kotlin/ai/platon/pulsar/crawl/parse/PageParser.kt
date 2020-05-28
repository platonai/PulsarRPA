/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.parse

import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.MetricsCounters
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.common.JobInitialized
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.filter.CrawlFilters
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.crawl.signature.Signature
import ai.platon.pulsar.crawl.signature.TextMD5Signature
import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.*
import kotlin.system.measureTimeMillis

class PageParser(
        val crawlFilters: CrawlFilters,
        val parserFactory: ParserFactory,
        val signature: Signature,
        val metricsCounters: MetricsCounters,
        val messageWriter: MiscMessageWriter,
        private val conf: ImmutableConfig
) : Parameterized, JobInitialized, AutoCloseable {

    enum class Counter { notFetched, alreadyParsed, truncated, notParsed, parseSuccess, parseFailed }
    init { MetricsCounters.register(Counter::class.java) }

    private val log = LoggerFactory.getLogger(PageParser::class.java)

    val unparsableTypes = ConcurrentSkipListSet<CharSequence>()
    private val maxParsedLinks = conf.getUint(CapabilityTypes.PARSE_MAX_LINKS_PER_PAGE, 200)
    /**
     * Parser timeout set to 30 sec by default. Set -1 (or any negative int) to deactivate
     */
    private val maxParseTime = conf.getDuration(CapabilityTypes.PARSE_TIMEOUT, AppConstants.DEFAULT_MAX_PARSE_TIME)
    val linkFilter = LinkFilter(crawlFilters, conf)

    /**
     * @param conf The configuration
     */
    constructor(conf: ImmutableConfig) : this(
            CrawlFilters(conf),
            ParserFactory(conf),
            TextMD5Signature(),
            MetricsCounters(),
            MiscMessageWriter(conf),
            conf
    )

    init {
        params.merge(linkFilter.params).withLogger(LOG).info(true)
    }

    override fun setup(jobConf: ImmutableConfig) {

    }

    override fun getParams(): Params {
        return Params.of(
                "maxParseTime", maxParseTime,
                "maxParsedLinks", maxParsedLinks
        )
    }

    /**
     * Parses given web page and stores parsed content within page. Puts a meta-redirect to outlinks.
     *
     * @param page The web page
     * @return ParseResult
     * A non-null ParseResult contains the main result and status of this parse
     */
    fun parse(page: WebPage): ParseResult {
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
                    messageWriter.debugExtractedFields(page)
                    page.marks.putIfNotNull(Mark.PARSE, page.marks[Mark.FETCH])
                }
            }

            return parseResult
        } catch (e: Throwable) {
            LOG.error(Strings.stringifyException(e))
        }

        return ParseResult()
    }

    // TODO: optimization
    private fun filterLinks(page: WebPage, unfilteredLinks: Set<HypeLink>): Set<HypeLink> {
        return unfilteredLinks.asSequence()
                .filter { linkFilter.asPredicate(page).test(it) } // filter out invalid urls
                .sortedByDescending { it.anchor.length } // longer anchor comes first
                .take(maxParsedLinks)
                .toSet()
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
            applyParsers(page)
        } catch (e: ParserNotFound) {
            unparsableTypes.add(page.contentType)
            LOG.warn("No parser found for <" + page.contentType + ">\n" + e.message)

            return ParseResult.failed(ParseStatus.FAILED_NO_PARSER, page.contentType)
        } catch (e: Throwable) {
            return ParseResult.failed(e)
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
                parseResult = takeIf { maxParseTime.seconds > 0 }?.runParser(parser, page)?:parser.parse(page)
            }
            parseResult.parser = parser

            if (log.isDebugEnabled && millis > 10_000) {
                val m = page.pageModel
                log.debug("It takes {} to parse {}/{}/{} fields | {}", Duration.ofMillis(millis).readable(),
                        m.numNonBlankFields, m.numNonNullFields, m.numFields, page.url)
            }

            // Found a suitable parser and successfully parsed
            if (parseResult.isSuccess) {
                // TODO: use FlowState
                break
            }
        }

        return parseResult
    }

    private fun runParser(p: Parser, page: WebPage): ParseResult {
        return runBlocking {
            withTimeout(maxParseTime.toMillis()) {
                val deferred = async { p.parse(page) }
                deferred.await()
            }
        }
    }

    private fun processSuccess(page: WebPage, parseResult: ParseResult) {
        val prevSig = page.signature
        if (prevSig != null) {
            page.prevSignature = prevSig
        }
        page.setSignature(signature.calculate(page))

        processLinks(page, parseResult.hypeLinks)

        // collect page features to better detect page types
        parseResult.domStatistics?.let { messageWriter.reportDOMStatistics(page, it) }
    }

    private fun processRedirect(page: WebPage, parseStatus: ParseStatus) {
        val refreshHref = parseStatus.getArgOrDefault(ParseStatus.REFRESH_HREF, "")
        val newUrl = crawlFilters.normalizeToNull(refreshHref, UrlNormalizers.SCOPE_FETCHER)?:return

        page.addLiveLink(HypeLink(newUrl))
        page.metadata[Name.REDIRECT_DISCOVERED] = AppConstants.YES_STRING
        if (newUrl == page.url) {
            val refreshTime = parseStatus.getArgOrDefault(ParseStatus.REFRESH_TIME, "0").toInt()
            val reprUrl = URLUtil.chooseRepr(page.url, newUrl, refreshTime < AppConstants.PERM_REFRESH_TIME)
            page.reprUrl = reprUrl
        }
    }

    private fun processLinks(page: WebPage, unfilteredLinks: MutableSet<HypeLink>) {
        // Collect links
        // TODO : check the no-follow html tag directive
        val follow = (!page.metadata.contains(Name.NO_FOLLOW)
                || page.isSeed
                || page.hasMark(Mark.INJECT)
                || page.metadata.contains(Name.FORCE_FOLLOW)
                || page.variables.contains(Name.FORCE_FOLLOW.name))
        if (follow) {
            // val hypeLinks = filterLinks(page, unfilteredLinks)
            // TODO: too many filters, hard to debug, move all filters to a single filter, or just do it in ParserFilter
            val hypeLinks = unfilteredLinks
            // log.debug("Find {}/{} live links", hypeLinks.size, unfilteredLinks.size)
            page.setLiveLinks(hypeLinks)
            page.addHyperLinks(hypeLinks)
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
            metricsCounters.inc(counter)
        }
    }

    override fun close() {
        messageWriter.reportLabeledHyperLinks(ParseResult.labeledHypeLinks)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(PageParser::class.java)

        /**
         * Checks if the page's content is truncated.
         *
         * @param page The web page
         * @return If the page is truncated `true`. When it is not, or when
         * it could be determined, `false`.
         */
        fun isTruncated(page: WebPage): Boolean {
            val url = page.url
            val inHeaderSize = page.headers.contentLength
            if (inHeaderSize < 0) {
                LOG.trace("HttpHeaders.CONTENT_LENGTH is not available | $url")
                return false
            }

            val content = page.content
            if (content == null) {
                LOG.debug("Page content is null, url: $url")
                return false
            }

            val actualSize = content.limit()
            return inHeaderSize > actualSize
        }
    }
}
