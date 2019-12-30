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

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.filter.CrawlFilters
import ai.platon.pulsar.crawl.parse.ParseFilters
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.ParseResult.Companion.failed
import ai.platon.pulsar.crawl.parse.Parser
import ai.platon.pulsar.crawl.parse.html.HTMLMetaTags
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.crawl.parse.html.PrimerParser
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import org.apache.html.dom.HTMLDocumentImpl
import org.cyberneko.html.parsers.DOMFragmentParser
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.w3c.dom.DocumentFragment
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

/**
 * Html parser
 */
class HtmlParser(
        private val parseFilters: ParseFilters,
        private val conf: ImmutableConfig
) : Parser {
    private val LOG = LoggerFactory.getLogger(HtmlParser::class.java)
    private val defaultCharEncoding = conf.get(CapabilityTypes.PARSE_DEFAULT_ENCODING, "utf-8")
    private val cachingPolicy = conf.get(CapabilityTypes.PARSE_CACHING_FORBIDDEN_POLICY, AppConstants.CACHING_FORBIDDEN_CONTENT)
    private val primerParser = PrimerParser(conf)

    init {
        LOG.info(params.formatAsLine())
    }

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "defaultCharEncoding", defaultCharEncoding,
                "cachingPolicy", cachingPolicy,
                "parseFilters", parseFilters
        )
    }

    override fun parse(page: WebPage): ParseResult {
        return try {
            // The base url is set by protocol. Might be different from url if the request redirected.
            doParse(page)
        } catch (e: MalformedURLException) {
            failed(ParseStatusCodes.FAILED_MALFORMED_URL, e.message)
        } catch (e: Exception) {
            failed(ParseStatusCodes.FAILED_INVALID_FORMAT, e.message)
        }
    }

    @Throws(MalformedURLException::class, Exception::class)
    private fun doParse(page: WebPage): ParseResult {
        val baseUrl = page.baseUrl
        val baseURL = URL(baseUrl)
        if (page.encoding == null) {
            primerParser.detectEncoding(page)
        }

        val (document, documentFragment) = parseJsoup(baseUrl, page.contentAsSaxInputSource)
        val metaTags = parseMetaTags(baseURL, documentFragment, page)
        val parseResult = initParseResult(metaTags)
        if (parseResult.isFailed) {
            return parseResult
        }

        page.pageTitle = primerParser.getPageTitle(documentFragment)
        page.pageModel.clear()

        val parseContext = ParseContext(page, parseResult, metaTags, documentFragment, FeaturedDocument(document))

        // TODO: for NativeBrowser mode, we can
        if (page.fetchMode == FetchMode.NATIVE_RENDERER) {
            // the native renderer may have done the parsing work
        }

        // TODO: a better place to init page model
        parseFilters.filter(parseContext)

        return parseContext.parseResult
    }

    private fun parseMetaTags(baseURL: URL, docRoot: DocumentFragment, page: WebPage): HTMLMetaTags {
        val metaTags = HTMLMetaTags(docRoot, baseURL)
        val tags = metaTags.generalTags
        val metadata = page.metadata
        tags.names().forEach { name: String -> metadata["meta_$name"] = tags[name] }
        if (metaTags.noCache) {
            metadata[CapabilityTypes.CACHING_FORBIDDEN_KEY] = cachingPolicy
        }
        return metaTags
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

    @Throws(IOException::class)
    private fun parseJsoup(baseUri: String, input: InputSource): Pair<Document, DocumentFragment> {
        val doc = Jsoup.parse(input.byteStream, input.encoding, baseUri)
        val documentFragment = W3CDom().fromJsoup(doc).createDocumentFragment()
        return doc to documentFragment
    }

    @Deprecated(message = "Use jsoup instead")
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
