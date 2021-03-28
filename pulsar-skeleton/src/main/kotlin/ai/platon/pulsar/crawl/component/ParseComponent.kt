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
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.filter.CrawlFilters
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Parser checker, useful for testing parser. It also accurately reports
 * possible fetching and parsing failures and presents protocol status signals
 * to aid debugging. The tool enables us to retrieve the following data from any
 */
@Component
class ParseComponent(
        val crawlFilters: CrawlFilters,
        val pageParser: PageParser,
        val conf: ImmutableConfig
) {
    private val logger = LoggerFactory.getLogger(ParseComponent::class.java)
    private var traceInfo: ConcurrentHashMap<String, Any>? = null

    constructor(conf: ImmutableConfig): this(CrawlFilters(conf), PageParser(conf), conf)

    fun parse(page: WebPage, reparseLinks: Boolean = false, noLinkFilter: Boolean = false): ParseResult {
        return parse(page, "", reparseLinks, noLinkFilter)
    }

    fun parse(page: WebPage, query: String?, reparseLinks: Boolean, noLinkFilter: Boolean): ParseResult {
        beforeParse(page, query, reparseLinks, noLinkFilter)
        return pageParser.parse(page).also { afterParse(page) }
    }

    private fun beforeParse(page: WebPage, query: String?, reparseLinks: Boolean, noLinkFilter: Boolean) {
        if (reparseLinks) {
            page.variables[Name.FORCE_FOLLOW] = AppConstants.YES_STRING
            page.variables[Name.REPARSE_LINKS] = AppConstants.YES_STRING
            page.variables[Name.PARSE_LINK_FILTER_DEBUG_LEVEL] = 1
        }
        if (noLinkFilter) {
            page.variables[Name.PARSE_NO_LINK_FILTER] = AppConstants.YES_STRING
        }
        page.query = query
        traceInfo?.clear()
    }

    private fun afterParse(page: WebPage) {
        page.variables.remove(Name.REPARSE_LINKS)
        page.variables.remove(Name.FORCE_FOLLOW)
        page.variables.remove(Name.PARSE_LINK_FILTER_DEBUG_LEVEL)
        page.variables.remove(Name.PARSE_NO_LINK_FILTER)
        page.query = null
    }

    fun getTraceInfo(): Map<String, Any> {
        if (traceInfo == null) {
            traceInfo = ConcurrentHashMap()
        }

        traceInfo?.also {
            it.clear()
            it["linkFilterReport"] = pageParser.linkFilter.filterReport.joinToString("\n") { it }
        }

        return traceInfo?: mapOf()
    }
}
