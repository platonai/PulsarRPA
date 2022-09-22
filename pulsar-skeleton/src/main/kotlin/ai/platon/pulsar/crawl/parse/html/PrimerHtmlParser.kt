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
package ai.platon.pulsar.crawl.parse.html

import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_DEFAULT_ENCODING
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.persist.ext.loadEvent
import ai.platon.pulsar.crawl.parse.ParseFilters
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.Parser
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.ParseStatusCodes
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Html parser
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
