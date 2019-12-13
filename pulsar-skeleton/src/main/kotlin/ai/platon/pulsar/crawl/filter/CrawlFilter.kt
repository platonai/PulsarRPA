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

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.Urls.reverseUrl
import ai.platon.pulsar.common.config.Configurable
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.PageCategory
import com.google.gson.GsonBuilder
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.conf.Configured
import org.w3c.dom.Node
import java.util.regex.Pattern
import java.util.stream.Stream

/**
 * TODO : configurable
 */
class CrawlFilter(val conf: ImmutableConfig) {
    var pageType = PageCategory.UNKNOWN
    var urlFilter: String? = null
    var textFilter: TextFilter? = null
    var blockFilter: BlockFilter? = null
    var startKey: String? = null
    var endKey: String? = null
    var reversedStartKey: String? = null
        private set
    var reversedEndKey: String? = null
        private set

    init {
        try {
            //      filter = conf.getBoolean(CRAWL_FILTER_FILTER, true);
            //      if (filter) {
            //        filters = new UrlFilters(conf);
            //      }
            //      normalise = conf.getBoolean(CRAWL_FILTER_NORMALISE, true);
            //      if (normalise) {
            //        normalizers = new UrlNormalizers(conf, UrlNormalizers.SCOPE_GENERATE_HOST_COUNT);
            //      }
            // CrawlFilter specified RegexURLFilter
            if (urlFilter != null) { // urlFilter = new RegexURLFilter(urlRegexRule);
            }
            // TODO : move to Table utils just like persist project does
            if (startKey != null) {
                startKey = startKey!!.replace("\\u0001".toRegex(), "\u0001")
                startKey = startKey!!.replace("\\\\u0001".toRegex(), "\u0001")
                reversedStartKey = reverseUrl(startKey!!)
            }
            if (endKey != null) {
                endKey = endKey!!.replace("\\uFFFF".toRegex(), "\uFFFF")
                endKey = endKey!!.replace("\\\\uFFFF".toRegex(), "\uFFFF")
                reversedEndKey = reverseUrl(endKey!!)
            }
        } catch (e: RuntimeException) {
            LOG.error(StringUtil.stringifyException(e))
        }
    }

    fun filter(page: WebPage): WebPage {
        return page
    }

    fun testUrlSatisfied(url: String?): Boolean {
        return url != null
        //    try {
//      if (normalizers != null) {
//        url = normalizers.normalize(url, UrlNormalizers.SCOPE_DEFAULT);
//        passed = url != null;
//      }
//
//      if (passed && filters != null) {
//        url = filters.filter(url);
//        passed = url != null;
//      }
//
//      if (passed && urlFilter != null) {
//        url = urlFilter.filter(url);
//        passed = url != null;
//      }
//    } catch (MalformedURLException|UrlFilterException e) {
//      log.error(e.toString());
//      passed = false;
//    }
//    if (passed && urlFilter != null) {
//      url = urlFilter.filter(url);
//      passed = url != null;
//    }
    }

    fun testKeyRangeSatisfied(reversedUrl: String?): Boolean {
        return keyGreaterEqual(reversedUrl, reversedStartKey) && keyLessEqual(reversedUrl, reversedEndKey)
    }

    fun testTextSatisfied(text: String?): Boolean {
        if (text == null) {
            return false
        }
        return if (textFilter == null) {
            true
        } else textFilter!!.test(text)
    }

    fun isAllowed(node: Node): Boolean {
        return if (blockFilter == null) {
            true
        } else blockFilter!!.isAllowed(node)
    }

    fun isDisallowed(node: Node): Boolean {
        return !isAllowed(node)
    }

    fun isDetailUrl(url: String): Boolean {
        return pageType == PageCategory.DETAIL && testUrlSatisfied(url)
    }

    fun isSearchUrl(url: String): Boolean {
        return pageType == PageCategory.SEARCH && testUrlSatisfied(url)
    }

    fun isMediaUrl(url: String): Boolean {
        return pageType == PageCategory.MEDIA && testUrlSatisfied(url)
    }

    fun isIndexUrl(url: String): Boolean {
        return pageType == PageCategory.INDEX && testUrlSatisfied(url)
    }

    override fun toString(): String {
        val gson = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation().create()
        return gson.toJson(this)
    }

    companion object {
        val LOG = CrawlFilters.LOG
        /**
         * TODO : use suffix-urlfilter instead
         */
        val MEDIA_URL_SUFFIXES = arrayOf("js", "css", "jpg", "png", "jpeg", "gif")
        /**
         * The follow patterns are simple rule to indicate a url's category, this is a very simple solution, and the result is
         * not accurate
         */
        var INDEX_PAGE_URL_PATTERNS = arrayOf(
                Pattern.compile(".+tieba.baidu.com/.+search.+"),
                Pattern.compile(".+(index|list|tags|chanel).+")
        )
        var SEARCH_PAGE_URL_PATTERN = Pattern.compile(".+(search|query|select).+")
        var DETAIL_PAGE_URL_PATTERNS = arrayOf(
                Pattern.compile(".+tieba.baidu.com/p/(\\d+)"),
                Pattern.compile(".+(detail|item|article|book|good|product|thread|view|post|content|/20[012][0-9]/{0,1}[01][0-9]/|/20[012]-[0-9]{0,1}-[01][0-9]/|/\\d{2,}/\\d{5,}|\\d{7,}).+")
        )
        var MEDIA_PAGE_URL_PATTERN = Pattern.compile(".+(pic|picture|photo|avatar|photoshow|video).+")

        /**
         * A simple regex rule to sniff the possible category of a web page
         */
        fun sniffPageCategory(urlString: String): PageCategory {
            if (urlString.isEmpty()) {
                return PageCategory.UNKNOWN
            }

            var pageCategory = PageCategory.UNKNOWN

            val url = urlString.toLowerCase()
            // Notice : ***DO KEEP*** the right order
            when {
                url.endsWith("/") -> {
                    pageCategory = PageCategory.INDEX
                }
                StringUtils.countMatches(url, "/") <= 3 -> { // http://t.tt/12345678
                    pageCategory = PageCategory.INDEX
                }
                INDEX_PAGE_URL_PATTERNS.any { it.matcher(url).matches() } -> {
                    pageCategory = PageCategory.INDEX
                }
                DETAIL_PAGE_URL_PATTERNS.any { it.matcher(url).matches() } -> {
                    pageCategory = PageCategory.DETAIL
                }
                SEARCH_PAGE_URL_PATTERN.matcher(url).matches() -> {
                    pageCategory = PageCategory.SEARCH
                }
                MEDIA_PAGE_URL_PATTERN.matcher(url).matches() -> {
                    pageCategory = PageCategory.MEDIA
                }
            }
            return pageCategory
        }

        fun keyGreaterEqual(test: String?, bound: String?): Boolean {
            if (test == null) {
                return false
            }
            return if (bound == null) {
                true
            } else test >= bound
        }

        fun keyLessEqual(test: String?, bound: String?): Boolean {
            if (test == null) {
                return false
            }
            return if (bound == null) {
                true
            } else test <= bound
        }
    }
}