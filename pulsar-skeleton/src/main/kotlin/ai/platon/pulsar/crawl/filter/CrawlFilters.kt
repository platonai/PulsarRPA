/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.filter

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.metadata.PageCategory
import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import java.util.*

/**
 * TODO : need full unit test
 * TODO : Move to plugin, urlfilter/contentfilter, etc
 */
class CrawlFilters(
        val crawlFilters: List<CrawlFilter>,
        val urlNormalizers: UrlNormalizers,
        val urlFilters: UrlFilters,
        val scope: String,
        val conf: ImmutableConfig
) {
    constructor(conf: ImmutableConfig) : this(
            listOf(),
            UrlNormalizers(conf),
            UrlFilters(conf),
            UrlNormalizers.SCOPE_DEFAULT,
            conf
    )

    fun isNormalizedValid(hyperlink: HyperlinkPersistable): Boolean {
        return isNormalizedValid(hyperlink.url)
    }

    fun isNormalizedValid(url: String): Boolean {
        return normalizeToEmpty(url).isNotEmpty()
    }

    fun normalizeToEmpty(url: String, scope: String = UrlNormalizers.SCOPE_DEFAULT): String {
        return normalizeToEmpty(url)
    }

    fun normalizeToEmpty(url: String): String {
        val normUrl = normalizeToNull(url)
        return normUrl ?: ""
    }

    fun normalizeToNull(url: String, scope: String = UrlNormalizers.SCOPE_DEFAULT): String? {
        if (url.isEmpty()) {
            return null
        }

        var filteredUrl: String? = temporaryUrlFilter(url)
        if (filteredUrl == null || filteredUrl.isEmpty()) {
            return null
        }

        // apply noNorm and noFilter
        filteredUrl = urlNormalizers.normalize(filteredUrl, scope)
        if (filteredUrl != null) {
            filteredUrl = urlFilters.filter(filteredUrl)
        }
        return filteredUrl
    }

    // TODO : use suffix-urlfilter instead, this is a quick dirty fix
    private fun temporaryUrlFilter(url_: String): String {
        var url = url_
        if (CrawlFilter.MEDIA_URL_SUFFIXES.any { url.endsWith(it) }) {
            url = ""
        }
        if (veryLikelyBeSearchUrl(url) || veryLikelyBeMediaUrl(url)) {
            url = ""
        }
        return url
    }

    fun testUrlSatisfied(url: String?): Boolean {
        if (url == null) return false
        for (filter in crawlFilters) {
            if (!filter.testUrlSatisfied(url)) {
                return false
            }
        }
        return true
    }

    fun testTextSatisfied(text: String?): Boolean {
        if (text == null) return false
        for (filter in crawlFilters) {
            if (!filter.testTextSatisfied(text)) {
                return false
            }
        }
        return true
    }

    fun testKeyRangeSatisfied(reversedUrl: String?): Boolean {
        if (reversedUrl == null) return false
        for (filter in crawlFilters) {
            if (filter.testKeyRangeSatisfied(reversedUrl)) {
                return true
            }
        }
        return true
    }

    val reversedKeyRanges: Map<String, String?>
        get() {
            val keyRanges: MutableMap<String, String?> = HashMap()
            for (filter in crawlFilters) {
                val reversedStartKey = filter.reversedStartKey
                val reversedEndKey = filter.reversedEndKey
                if (reversedStartKey != null) {
                    keyRanges[reversedStartKey] = reversedEndKey
                }
            }
            return keyRanges
        }

    /**
     * TODO: use pair
     */
    val maxReversedKeyRange: Array<String?>
        get() {
            val keyRange = arrayOf<String?>(null, null)
            for (filter in crawlFilters) {
                val reversedStartKey = filter.reversedStartKey
                val reversedEndKey = filter.reversedEndKey
                if (reversedStartKey != null) {
                    if (keyRange[0] == null) {
                        keyRange[0] = reversedStartKey
                    } else if (CrawlFilter.keyLessEqual(reversedStartKey, keyRange[0])) {
                        keyRange[0] = reversedStartKey
                    }
                }
                if (reversedEndKey != null) {
                    if (keyRange[1] == null) {
                        keyRange[1] = reversedEndKey
                    } else if (CrawlFilter.keyGreaterEqual(reversedEndKey, keyRange[1])) {
                        keyRange[1] = reversedEndKey
                    }
                }
            }
            return keyRange
        }

    /**
     * TODO : Tricky logic
     */
    fun isAllowed(node: Node?): Boolean {
        if (node == null) {
            return false
        }
        if (crawlFilters.isEmpty()) {
            return true
        }
        for (filter in crawlFilters) {
            if (filter.isAllowed(node)) {
                return true
            }
        }
        return false
    }

    /**
     * TODO : Tricky logic
     */
    fun isDisallowed(node: Node?): Boolean {
        if (node == null) {
            return true
        }
        if (isAllowed(node)) {
            return false
        }
        for (filter in crawlFilters) {
            if (filter.isDisallowed(node)) {
                return true
            }
        }
        return false
    }

    fun veryLikelyBeDetailUrl(url: String?): Boolean {
        if (url == null) {
            return false
        }
        val pageType = CrawlFilter.guessPageCategory(url)
        if (pageType == PageCategory.DETAIL) {
            return true
        }
        for (filter in crawlFilters) {
            if (filter.isDetailUrl(url)) {
                return true
            }
        }
        return false
    }

    fun veryLikelyBeIndexUrl(url: String?): Boolean {
        if (url == null) return false
        val pageType = CrawlFilter.guessPageCategory(url)
        if (pageType.isIndex) {
            return true
        }
        for (filter in crawlFilters) {
            if (filter.isIndexUrl(url)) {
                return true
            }
        }
        return false
    }

    fun veryLikelyBeMediaUrl(url: String?): Boolean {
        if (url == null) return false
        val pageType = CrawlFilter.guessPageCategory(url)
        if (pageType == PageCategory.MEDIA) {
            return true
        }
        for (filter in crawlFilters) {
            if (filter.isMediaUrl(url)) {
                return true
            }
        }
        return false
    }

    /**
     * Notice : index url is not a search url even if it contains "search"
     */
    fun veryLikelyBeSearchUrl(url: String?): Boolean {
        if (url == null) {
            return false
        }
        val pageType = CrawlFilter.guessPageCategory(url)
        if (pageType == PageCategory.SEARCH) {
            return true
        }
        for (filter in crawlFilters) {
            if (filter.isSearchUrl(url)) {
                return true
            }
        }
        return false
    }

    fun toJson(): String {
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        return gson.toJson(this)
    }

    override fun toString(): String {
        return toJson()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(CrawlFilters::class.java)
        const val CRAWL_FILTER_RULES = "crawl.filter.rules"
    }
}
