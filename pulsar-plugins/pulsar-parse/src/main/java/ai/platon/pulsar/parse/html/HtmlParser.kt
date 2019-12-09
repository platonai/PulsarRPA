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
package ai.platon.pulsar.parse.html

import ai.platon.pulsar.common.EncodingDetector
import ai.platon.pulsar.common.MetricsSystem
import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.crawl.filter.CrawlFilters
import ai.platon.pulsar.crawl.parse.ParseFilters
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.ParseResult.Companion.failed
import ai.platon.pulsar.crawl.parse.Parser
import ai.platon.pulsar.crawl.parse.html.HTMLMetaTags
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.crawl.parse.html.PrimerParser
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import org.apache.html.dom.HTMLDocumentImpl
import org.cyberneko.html.parsers.DOMFragmentParser
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.w3c.dom.DocumentFragment
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.function.Consumer

/**
 * Html parser
 */
class HtmlParser(
        private val webDb: WebDb,
        private val crawlFilters: CrawlFilters,
        private val parseFilters: ParseFilters,
        private var conf: ImmutableConfig
) : Parser {
    private var parserImpl: String? = null
    private var defaultCharEncoding: String? = null
    private var encodingDetector: EncodingDetector? = null
    private var primerParser: PrimerParser? = null
    private var cachingPolicy: String? = null
    private var metricsSystem: MetricsSystem? = null

    init {
        reload(conf)
    }

    override fun reload(conf: ImmutableConfig) {
        this.conf = conf
        parserImpl = conf[CapabilityTypes.PARSE_HTML_IMPL, "neko"]
        defaultCharEncoding = conf[CapabilityTypes.PARSE_DEFAULT_ENCODING, "utf-8"]
        cachingPolicy = conf[CapabilityTypes.PARSE_CACHING_FORBIDDEN_POLICY, PulsarConstants.CACHING_FORBIDDEN_CONTENT]
        primerParser = PrimerParser(conf)
        encodingDetector = EncodingDetector(conf)
        metricsSystem = MetricsSystem(webDb, conf)
        Parser.LOG.info(params.formatAsLine())
        Parser.LOG.info("Active parse filters : $parseFilters")
    }

    override fun getConf(): ImmutableConfig {
        return conf
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "parserImpl", parserImpl,
                "defaultCharEncoding", defaultCharEncoding,
                "cachingPolicy", cachingPolicy
        )
    }

    override fun parse(page: WebPage): ParseResult {
        return try { // The base url is set by protocol. Might be different from url if the request redirected.
            val url = page.url
            val baseUrl = page.baseUrl
            val baseURL = URL(baseUrl)
            if (page.encoding == null) {
                primerParser!!.detectEncoding(page)
            }
            val inputSource = page.contentAsSaxInputSource
            val documentFragment = parse(baseUrl, inputSource)
            val metaTags = parseMetaTags(baseURL, documentFragment, page)
            val parseResult = initParseResult(metaTags)
            if (parseResult.isFailed) {
                return parseResult
            }
            val parseContext = ParseContext(page, metaTags, documentFragment, parseResult)
            parseLinks(baseURL, parseContext)
            page.pageTitle = primerParser!!.getPageTitle(documentFragment)
            page.pageText = primerParser!!.getPageText(documentFragment)
            page.pageModel.clear()
            parseFilters.filter(parseContext)
            parseContext.parseResult
        } catch (e: MalformedURLException) {
            failed(ParseStatusCodes.FAILED_MALFORMED_URL, e.message)
        } catch (e: Exception) {
            failed(ParseStatusCodes.FAILED_INVALID_FORMAT, e.message)
        }
    }

    private fun parseMetaTags(baseURL: URL, docRoot: DocumentFragment, page: WebPage): HTMLMetaTags {
        val metaTags = HTMLMetaTags(docRoot, baseURL)
        val tags = metaTags.generalTags
        val metadata = page.metadata
        tags.names().forEach(Consumer { name: String -> metadata["meta_$name"] = tags[name] })
        if (metaTags.noCache) {
            metadata[CapabilityTypes.CACHING_FORBIDDEN_KEY] = cachingPolicy
        }
        return metaTags
    }

    private fun parseLinks(baseURL: URL, parseContext: ParseContext) {
        var baseURL: URL? = baseURL
        var url = parseContext.url
        url = crawlFilters.normalizeToEmpty(url)
        if (url.isEmpty()) {
            return
        }
        val page = parseContext.page
        val metaTags = parseContext.metaTags
        val docRoot = parseContext.documentFragment
        val parseResult = parseContext.parseResult
        if (!metaTags.noFollow) { // okay to follow links
            val baseURLFromTag = primerParser!!.getBaseURL(docRoot)
            baseURL = baseURLFromTag ?: baseURL
            primerParser!!.getLinks(baseURL, parseResult.hypeLinks, docRoot, crawlFilters)
        }
        page.increaseImpreciseLinkCount(parseResult.hypeLinks.size)
        page.variables[PulsarParams.VAR_LINKS_COUNT] = parseResult.hypeLinks.size
    }

    private fun initParseResult(metaTags: HTMLMetaTags): ParseResult {
        if (metaTags.noIndex) {
            return ParseResult(ParseStatus.SUCCESS, ParseStatus.SUCCESS_NO_INDEX)
        }
        val parseResult = ParseResult(ParseStatus.SUCCESS, ParseStatus.SUCCESS_OK)
        if (metaTags.refresh) {
            parseResult.minorCode = ParseStatus.SUCCESS_REDIRECT
            parseResult.args[ParseStatus.REFRESH_HREF] = metaTags.refreshHref.toString()
            parseResult.args[ParseStatus.REFRESH_TIME] = Integer.toString(metaTags.refreshTime)
        }
        return parseResult
    }

    @Throws(Exception::class)
    private fun parse(baseUri: String, input: InputSource): DocumentFragment {
        return if (parserImpl.equals("neko", ignoreCase = true)) {
            parseNeko(baseUri, input)
        } else {
            parseJsoup(baseUri, input)
        }
    }

    @Throws(IOException::class)
    private fun parseJsoup(baseUri: String, input: InputSource): DocumentFragment {
        val doc = Jsoup.parse(input.byteStream, input.encoding, baseUri)
        val dom = W3CDom()
        return dom.fromJsoup(doc).createDocumentFragment()
    }

    @Throws(Exception::class)
    private fun parseNeko(baseUri: String, input: InputSource): DocumentFragment {
        val parser = DOMFragmentParser()
        try {
            parser.setFeature("http://cyberneko.org/html/features/scanner/allow-selfclosing-iframe", true)
            parser.setFeature("http://cyberneko.org/html/features/augmentations", true)
            parser.setProperty("http://cyberneko.org/html/properties/default-encoding", defaultCharEncoding)
            parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true)
            parser.setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", false)
            parser.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true)
            parser.setFeature("http://cyberneko.org/html/features/report-errors", Parser.LOG.isTraceEnabled)
        } catch (ignored: SAXException) {
        }
        // convert Document to DocumentFragment
        val doc = HTMLDocumentImpl()
        doc.errorChecking = false
        val res = doc.createDocumentFragment()
        var frag = doc.createDocumentFragment()
        parser.parse(input, frag)
        res.appendChild(frag)
        try {
            while (true) {
                frag = doc.createDocumentFragment()
                parser.parse(input, frag)
                if (!frag.hasChildNodes()) break
                if (Parser.LOG.isInfoEnabled) {
                    Parser.LOG.info(" - new frag, " + frag.childNodes.length + " nodes.")
                }
                res.appendChild(frag)
            }
        } catch (x: Exception) {
            Parser.LOG.error("Failed with the following Exception: ", x)
        }
        return res
    }
}