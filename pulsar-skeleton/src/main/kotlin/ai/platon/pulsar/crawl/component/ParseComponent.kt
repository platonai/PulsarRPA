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
package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.filter.CrawlFilters
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * The parse component.
 */
class ParseComponent(
        val crawlFilters: CrawlFilters,
        val pageParser: PageParser,
        val globalCacheFactory: GlobalCacheFactory,
        val conf: ImmutableConfig
) {
    companion object {
        val numParses = AtomicInteger()
        val numParsed = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(ParseComponent::class.java)
    private var traceInfo: ConcurrentHashMap<String, Any>? = null

    constructor(globalCacheFactory: GlobalCacheFactory, conf: ImmutableConfig): this(CrawlFilters(conf), PageParser(conf), globalCacheFactory, conf)

    fun parse(page: WebPage, reparseLinks: Boolean = false, noLinkFilter: Boolean = true): ParseResult {
        beforeParse(page, reparseLinks, noLinkFilter)
        return pageParser.parse(page).also { afterParse(page, it) }
    }

    private fun beforeParse(page: WebPage, reparseLinks: Boolean, noLinkFilter: Boolean) {
        numParses.incrementAndGet()

        if (reparseLinks) {
            page.variables[Name.FORCE_FOLLOW] = AppConstants.YES_STRING
            page.variables[Name.REPARSE_LINKS] = AppConstants.YES_STRING
            page.variables[Name.PARSE_LINK_FILTER_DEBUG_LEVEL] = 1
        }
        if (noLinkFilter) {
            page.variables[Name.PARSE_NO_LINK_FILTER] = AppConstants.YES_STRING
        }
        traceInfo?.clear()
    }

    private fun afterParse(page: WebPage, result: ParseResult) {
        page.variables.remove(Name.REPARSE_LINKS)
        page.variables.remove(Name.FORCE_FOLLOW)
        page.variables.remove(Name.PARSE_LINK_FILTER_DEBUG_LEVEL)
        page.variables.remove(Name.PARSE_NO_LINK_FILTER)

        val document = result.document
        if (document != null) {
            globalCacheFactory.globalCache.documentCache.putDatum(page.url, document)
        }

        numParsed.incrementAndGet()
    }

    fun getTraceInfo(): Map<String, Any> {
        if (traceInfo == null) {
            traceInfo = ConcurrentHashMap()
        }

        traceInfo?.also {
            it.clear()
        }

        return traceInfo?: mapOf()
    }
}
