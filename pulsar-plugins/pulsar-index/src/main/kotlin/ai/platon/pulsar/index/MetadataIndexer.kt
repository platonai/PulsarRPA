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
package ai.platon.pulsar.index

import ai.platon.pulsar.crawl.common.URLUtil.getDomainName
import ai.platon.pulsar.crawl.index.IndexingFilter
import ai.platon.pulsar.common.config.ImmutableConfig
import kotlin.Throws
import ai.platon.pulsar.crawl.index.IndexingException
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.persist.WebPage
import java.net.MalformedURLException
import ai.platon.pulsar.common.DateTimes.isoInstantFormat
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.WebPageExt
import java.net.URL
import java.time.Instant
import java.util.*
import java.util.function.Consumer

/**
 * Indexer which can be configured to extract metadata from the crawldb, parse
 * metadata or content metadata. You can specify the properties "index.db",
 * "index.parse" or "index.content" who's values are comma-delimited
 * <value>key1,key2,key3</value>.
 */
class MetadataIndexer(
    override var conf: ImmutableConfig
) : IndexingFilter {

    override fun setup(conf: ImmutableConfig) {
        this.conf = conf
        conf.getStringCollection(PARSE_CONF_PROPERTY).forEach(Consumer { metatag: String ->
            val key = PARSE_META_PREFIX + metatag.toLowerCase(Locale.ROOT)
            val value = INDEX_PREFIX + metatag
            parseFieldnames[key] = value
        })
    }

    override fun getParams(): Params {
        return Params()
    }

    @Throws(IndexingException::class)
    override fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument? {
        try {
            addTime(doc, url, page)
            addHost(doc, url, page)

            // MultiMetadata-index does not meet all our requirement
            addGeneralMetadata(doc, url, page)
            addPageMetadata(doc, url, page)
        } catch (e: IndexingException) {
            IndexingFilter.LOG.error(e.toString())
        }
        return doc
    }

    @Throws(IndexingException::class)
    private fun addHost(doc: IndexDocument, url: String, page: WebPage) {
        var url: String? = url
        val reprUrlString = page.reprUrl
        url = if (reprUrlString.isEmpty()) url else reprUrlString
        if (url == null || url.isEmpty()) {
            return
        }
        try {
            val u = URL(url)
            val domain = getDomainName(u)
            doc.add("url", url)
            doc.add("domain", domain)
            doc.addIfNotNull("host", u.host)
        } catch (e: MalformedURLException) {
            throw IndexingException(e)
        }
    }

    private fun addTime(doc: IndexDocument, url: String, page: WebPage) {
        val pageExt = WebPageExt(page)
        val now = Instant.now()
        val crawlTimeStr = isoInstantFormat(now)
        val firstFetchTime = pageExt.firstFetchTime ?: now
        val fetchTimeHistory = page.getFetchTimeHistory(crawlTimeStr)
        doc.add("first_crawl_time", isoInstantFormat(firstFetchTime))
        doc.add("last_crawl_time", crawlTimeStr)
        doc.add("fetch_time_history", fetchTimeHistory)
        val indexTimeStr = isoInstantFormat(now)
        val firstIndexTime = pageExt.getFirstIndexTime(now)
        val indexTimeHistory = page.getIndexTimeHistory(indexTimeStr)
        doc.add("first_index_time", isoInstantFormat(firstIndexTime))
        doc.add("last_index_time", indexTimeStr)
        doc.add("index_time_history", indexTimeHistory)
    }

    @Throws(IndexingException::class)
    private fun addGeneralMetadata(doc: IndexDocument, url: String, page: WebPage) {
        val contentType = page.contentType
        if (!contentType.contains("html")) {
            IndexingFilter.LOG.warn("Content type $contentType is not fully supported")
            // return doc;
        }

        // get content type
        doc.add("content_type", contentType)
    }

    private fun addPageMetadata(doc: IndexDocument?, url: String, page: WebPage): IndexDocument? {
        if (doc == null || parseFieldnames.isEmpty()) {
            return doc
        }
        for (metatag in parseFieldnames.entries) {
            var k = metatag.value
            var metadata = page.metadata[metatag.key]
            if (k != null && metadata != null) {
                k = k.trim { it <= ' ' }
                metadata = metadata.trim { it <= ' ' }
                if (!k.isEmpty() && !metadata.isEmpty()) {
                    val finalK = k

                    // TODO : avoid this dirty hard coding
                    if (finalK.equals("meta_description", ignoreCase = true)) {
                        Arrays.stream(metadata.split("\t".toRegex()).toTypedArray())
                            .forEach { v: String? -> doc.addIfAbsent(finalK, v!!) }
                    } else {
                        Arrays.stream(metadata.split("\t".toRegex()).toTypedArray())
                            .forEach { v: String? -> doc.add(finalK, v) }
                    }
                }
            } // if
        } // for
        return doc
    }

    companion object {
        private const val PARSE_CONF_PROPERTY = "index.metadata"
        private const val INDEX_PREFIX = "meta_"
        private const val PARSE_META_PREFIX = "meta_"
        private val parseFieldnames: MutableMap<String, String> = TreeMap()
    }
}
