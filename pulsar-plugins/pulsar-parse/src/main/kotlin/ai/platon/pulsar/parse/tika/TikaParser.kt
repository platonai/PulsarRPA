/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.parse.tika

import ai.platon.pulsar.common.ReflectionUtils
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_CACHING_FORBIDDEN_POLICY
import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_TIKA_HTML_MAPPER_NAME
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.filter.CrawlFilters
import ai.platon.pulsar.skeleton.crawl.parse.ParseFilters
import ai.platon.pulsar.skeleton.crawl.parse.ParseResult
import ai.platon.pulsar.skeleton.crawl.parse.ParseResult.Companion.failed
import ai.platon.pulsar.skeleton.crawl.parse.Parser
import ai.platon.pulsar.skeleton.crawl.parse.html.HTMLMetaTags
import ai.platon.pulsar.skeleton.crawl.parse.html.PrimerParser
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import org.apache.html.dom.HTMLDocumentImpl
import org.apache.tika.config.TikaConfig
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.html.HtmlMapper
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.MalformedURLException
import java.net.URL

/**
 * Wrapper for Tika parsers. Mimics the HTMLParser but using the XHTML
 * representation returned by Tika as SAX events
 */
class TikaParser(
        val crawlFilters: CrawlFilters? = null,
        val parseFilters: ParseFilters? = null,
        val conf: ImmutableConfig
) : Parser {
    private val logger = LoggerFactory.getLogger(TikaParser::class.java)
    private val primerParser = PrimerParser(conf)

    private val tikaConfig = TikaConfig.getDefaultConfig()
    private val cachingPolicy = conf.get(PARSE_CACHING_FORBIDDEN_POLICY, AppConstants.CACHING_FORBIDDEN_CONTENT)
    private var htmlMapper = conf.get(PARSE_TIKA_HTML_MAPPER_NAME)?.let { ReflectionUtils.forNameOrNull<HtmlMapper>(it) }

    override val timeout = conf.getDuration(CapabilityTypes.PARSE_TIMEOUT, AppConstants.DEFAULT_MAX_PARSE_TIME)!!

    constructor(conf: ImmutableConfig): this(null, null, conf)

    override fun parse(page: WebPage): ParseResult {
        val baseUrl = page.location
        val base = try {
            URL(baseUrl)
        } catch (e: MalformedURLException) {
            return failed(e)
        }

        // get the right parser using the mime type as a clue
        val mimeType = page.contentType
        val tikamd = Metadata()
        val doc = HTMLDocumentImpl()
        doc.errorChecking = false
        val root = doc.createDocumentFragment()
        val domhandler = DOMBuilder(doc, root)
        val context = ParseContext()
        if (htmlMapper != null) {
            context.set(HtmlMapper::class.java, htmlMapper)
        }

        tikamd[Metadata.CONTENT_TYPE] = mimeType
        try {
            val raw = page.content
            if (raw != null) {
                tikaConfig.parser.parse(ByteArrayInputStream(raw.array(), raw.arrayOffset()
                        + raw.position(), raw.remaining()), domhandler, tikamd, context)
            }
        } catch (e: Exception) {
            logger.error("Error parsing " + page.url, e)
            return failed(e)
        }

        var pageTitle: String? = ""
        var pageText: String? = ""
        val hypeLinks = mutableSetOf<HyperlinkPersistable>()
        // we have converted the sax events generated by Tika into a DOM object
        val metaTags = HTMLMetaTags(root, base)
        // check meta directives
        if (!metaTags.noIndex) {
            // okay to index
            pageText = primerParser.getPageText(root) // extract text
            pageTitle = primerParser.getPageTitle(root) // extract title
        }

        if (!metaTags.noFollow) {// okay to follow links
            val baseTag = primerParser.getBaseURLFromTag(root)
            primerParser.collectLinks(baseTag ?: base, hypeLinks, root, null)
        }

        page.setPageTitle(pageTitle)
        page.setPageText(pageText)
        for (name in tikamd.names()) {
            if (name.equals(TikaCoreProperties.TITLE.toString(), ignoreCase = true)) {
                continue
            }
            page.metadata[name] = tikamd[name]
        }

        val parseResult = ParseResult(ParseStatusCodes.SUCCESS, ParseStatusCodes.SUCCESS_OK)
        if (metaTags.refresh) {
            parseResult.minorCode = ParseStatusCodes.SUCCESS_REDIRECT
            parseResult.args[ParseStatus.REFRESH_HREF] = metaTags.refreshHref.toString()
            parseResult.args[ParseStatus.REFRESH_TIME] = Integer.toString(metaTags.refreshTime)
        }

        parseFilters?.filter(ai.platon.pulsar.skeleton.crawl.parse.html.ParseContext(page, parseResult))
        if (metaTags.noCache) {
            // not okay to cache
            page.metadata[CapabilityTypes.CACHING_FORBIDDEN_KEY] = cachingPolicy
        }

        return parseResult
    }
}
