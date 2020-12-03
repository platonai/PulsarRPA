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

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.HtmlDocumentHandler
import ai.platon.pulsar.crawl.WebPageHandler
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
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.w3c.dom.DocumentFragment
import org.xml.sax.InputSource
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
    private val log = LoggerFactory.getLogger(HtmlParser::class.java)
    private val tracer = log.takeIf { it.isDebugEnabled }
    private val defaultCharEncoding = conf.get(CapabilityTypes.PARSE_DEFAULT_ENCODING, "utf-8")
    private val cachingPolicy = conf.get(CapabilityTypes.PARSE_CACHING_FORBIDDEN_POLICY, AppConstants.CACHING_FORBIDDEN_CONTENT)
    private val primerParser = PrimerParser(conf)

    init {
        log.info(params.formatAsLine())
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
            // The base url is set by protocol. Might be different from url if the request redirected
            beforeParse(page)

            val parseContext = doParse(page)

            parseContext.document?.let { afterParse(page, it) }

            parseContext.parseResult
        } catch (e: MalformedURLException) {
            failed(ParseStatusCodes.FAILED_MALFORMED_URL, e.message)
        } catch (e: Exception) {
            failed(ParseStatusCodes.FAILED_INVALID_FORMAT, e.message)
        }
    }

    @Throws(MalformedURLException::class, Exception::class)
    private fun doParse(page: WebPage): ParseContext {
        tracer?.trace("{}.\tParsing page | {} | {} | {} | {}",
                page.id,
                Strings.readableBytes(page.contentBytes.toLong()),
                page.protocolStatus,
                page.htmlIntegrity,
                page.url)

        val baseUrl = page.baseUrl ?: page.url
        val baseURL = URL(baseUrl)
        if (page.encoding == null) {
            primerParser.detectEncoding(page)
        }

        val (document, documentFragment) = parseJsoup(baseUrl, page.contentAsSaxInputSource)
        val metaTags = parseMetaTags(baseURL, documentFragment, page)
        val parseResult = initParseResult(metaTags)

        val parseContext = ParseContext(page, parseResult, FeaturedDocument(document), metaTags, documentFragment)
        parseFilters.filter(parseContext)

        return parseContext
    }

    private fun beforeParse(page: WebPage) {
        page.volatileConfig?.getBean(CapabilityTypes.FETCH_BEFORE_HTML_PARSE_HANDLER, WebPageHandler::class.java)
                ?.runCatching { invoke(page) }
                ?.onFailure { log.warn("Failed to run before parse handler | {}", page.url) }
                ?.getOrNull()
    }

    private fun afterParse(page: WebPage, document: FeaturedDocument) {
        page.volatileConfig?.getBean(CapabilityTypes.FETCH_AFTER_HTML_PARSE_HANDLER, HtmlDocumentHandler::class.java)
                ?.runCatching { invoke(page, document) }
                ?.onFailure { log.warn("Failed to run after parse handler | {}", page.url) }
                ?.getOrNull()
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
            parseResult.args[ParseStatus.REFRESH_TIME] = metaTags.refreshTime.toString()
        }

        return parseResult
    }

    @Throws(IOException::class)
    private fun parseJsoup(baseUri: String, input: InputSource): Pair<Document, DocumentFragment> {
        val doc = Jsoup.parse(input.byteStream, input.encoding, baseUri)
        val documentFragment = W3CDom().fromJsoup(doc).createDocumentFragment()
        return doc to documentFragment
    }
}
