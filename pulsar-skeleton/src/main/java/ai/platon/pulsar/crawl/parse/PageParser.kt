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

import ai.platon.pulsar.common.MetricsCounters
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.URLUtil
import ai.platon.pulsar.common.config.*
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
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PageParser(
        val crawlFilters: CrawlFilters,
        val parserFactory: ParserFactory,
        val signature: Signature,
        val pulsarCounters: MetricsCounters,
        private val conf: ImmutableConfig
) : ReloadableParameterized {
    val unparsableTypes = Collections.synchronizedSet(HashSet<CharSequence>())
    private var maxParsedLinks = 0
    /**
     * Parser timeout set to 30 sec by default. Set -1 (or any negative int) to deactivate
     */
    private lateinit var maxParseTime: Duration
    lateinit var linkFilter: LinkFilter
    // TODO: merge with GlobalExecutor
    private var executorService: ExecutorService? = null

    /**
     * @param conf The configuration
     */
    constructor(conf: ImmutableConfig) : this(
            CrawlFilters(conf),
            ParserFactory(conf),
            TextMD5Signature(),
            MetricsCounters(),
            conf
    )

    override fun getConf(): ImmutableConfig {
        return conf
    }

    override fun getParams(): Params {
        return Params.of(
                "maxParseTime", maxParseTime,
                "maxParsedLinks", maxParsedLinks
        )
    }

    override fun reload(conf: ImmutableConfig) {
        maxParseTime = conf.getDuration(CapabilityTypes.PARSE_TIMEOUT, AppConstants.DEFAULT_MAX_PARSE_TIME)
        maxParsedLinks = conf.getUint(CapabilityTypes.PARSE_MAX_LINKS_PER_PAGE, 200)
        linkFilter = LinkFilter(crawlFilters, conf)
        if (maxParseTime.seconds > 0) {
            val threadFactory = ThreadFactoryBuilder().setNameFormat("parser-%d").setDaemon(true).build()
            executorService = Executors.newCachedThreadPool(threadFactory)
        }
        params.merge(linkFilter.params).withLogger(LOG).info(true)
    }

    /**
     * Parses given web page and stores parsed content within page. Puts a
     * meta-redirect to outlinks.
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
                    page.marks.putIfNotNull(Mark.PARSE, page.marks[Mark.FETCH])
                }
            }

            return parseResult
        } catch (e: Throwable) {
            LOG.error(StringUtil.stringifyException(e))
        }

        return ParseResult()
    }

    // TODO: optimization
    private fun filterLinks(page: WebPage, unfilteredLinks: List<HypeLink>): List<HypeLink> {
        var sequence = 0
        return unfilteredLinks.asSequence()
                .filter { linkFilter.asPredicate(page).test(it) } // filter out invalid urls
                .onEach { it.order = ++sequence } // links added later, crawls earlier
                .sortedByDescending { it.anchor.length } // longer anchor comes first
                .toSet().take(maxParsedLinks)
    }

    private fun doParse(page: WebPage): ParseResult {
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
        var parseResult = ParseResult()

        val parsers = parserFactory.getParsers(page.contentType, page.url)

        for (parser in parsers) {
            parseResult = if (executorService != null) {
                runParser(parser, page)
            } else {
                parser.parse(page)
            }
            parseResult.parser = parser

            // Found a suitable parser and successfully parsed
            if (parseResult.isSuccess) {
                break
            }
        }

        return parseResult
    }

    private fun runParser(p: Parser, page: WebPage): ParseResult {
        var parseResult = ParseResult()
        val executor = executorService?:return parseResult

        val task = executor.submit<ParseResult> { p.parse(page) }
        try {
            parseResult = task.get(maxParseTime.seconds, TimeUnit.SECONDS)
        } catch (e: Exception) {
            LOG.warn("Failed to parsing " + page.url, e)
            task.cancel(true)
        }
        return parseResult
    }

    private fun processSuccess(page: WebPage, parseResult: ParseResult) {
        val prevSig = page.signature
        if (prevSig != null) {
            page.prevSignature = prevSig
        }
        page.setSignature(signature.calculate(page))
        // Collect links
        // TODO : check the no-follow html tag directive
        val follow = (!page.metadata.contains(Name.NO_FOLLOW)
                || page.isSeed
                || page.hasMark(Mark.INJECT)
                || page.metadata.contains(Name.FORCE_FOLLOW)
                || page.variables.contains(Name.FORCE_FOLLOW.name))
        if (follow) {
            processLinks(page, parseResult.hypeLinks)
        }
    }

    private fun processRedirect(page: WebPage, parseStatus: ParseStatus) {
        val refreshHref = parseStatus.getArgOrDefault(ParseStatus.REFRESH_HREF, "")
        val newUrl = crawlFilters.normalizeToEmpty(refreshHref, UrlNormalizers.SCOPE_FETCHER)
        if (newUrl.isEmpty()) {
            return
        }

        page.addLiveLink(HypeLink(newUrl))
        page.metadata[Name.REDIRECT_DISCOVERED] = AppConstants.YES_STRING
        if (newUrl == page.url) {
            val refreshTime = parseStatus.getArgOrDefault(ParseStatus.REFRESH_TIME, "0").toInt()
            val reprUrl = URLUtil.chooseRepr(page.url, newUrl, refreshTime < AppConstants.PERM_REFRESH_TIME)
            page.reprUrl = reprUrl
        }
    }

    private fun processLinks(page: WebPage, unfilteredLinks: ArrayList<HypeLink>) {
        val hypeLinks = filterLinks(page, unfilteredLinks)
        page.setLiveLinks(hypeLinks)
        page.addHyperLinks(hypeLinks)
    }

    // 0 : "notparsed", 1 : "success", 2 : "failed"
    private fun updateCounters(parseStatus: ParseStatus?) {
        if (parseStatus == null) {
            return
        }
        var counter: Counter? = null
        when (parseStatus.majorCode) {
            ParseStatusCodes.FAILED -> counter = Counter.parseFailed
            else -> {
            }
        }
        if (counter != null) {
            pulsarCounters.increase(counter)
        }
    }

    enum class Counter {
        notFetched, alreadyParsed, truncated, notParsed, parseSuccess, parseFailed
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
                LOG.debug("HttpHeaders.CONTENT_LENGTH is not available | $url")
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

        init {
            MetricsCounters.register(Counter::class.java)
        }
    }

    init {
        reload(conf)
    }
}