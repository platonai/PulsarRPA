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

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.crawl.index.IndexingFilter
import ai.platon.pulsar.index.AnchorIndexingFilter
import ai.platon.pulsar.persist.WebPage
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Indexing filter that offers an option to either index all inbound anchor text
 * for a document or deduplicate anchors. Deduplication does have it's con's,
 */
class AnchorIndexingFilter(
    override var conf: ImmutableConfig,
) : IndexingFilter {
    private var deduplicate = false

    override fun setup(conf: ImmutableConfig) {
        this.conf = conf
        deduplicate = conf.getBoolean("anchorIndexingFilter.deduplicate", true)
    }

    override fun getParams(): Params {
        return Params.of("anchor.indexing.filter.deduplicate", deduplicate)
    }

    /**
     * The [AnchorIndexingFilter] filter object which supports boolean
     * configuration settings for the deduplication of anchors. See
     * `anchorIndexingFilter.deduplicate` in pulsar-default.xml.
     *
     * @param doc  The [IndexDocument] object
     * @param url  URL to be filtered for anchor text
     * @param page [WebPage] object relative to the URL
     * @return filtered IndexDocument
     */
    override fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument? {
        var set: HashSet<String?>? = null
        for ((_, value) in page.inlinks) {
            val anchor = value.toString()
            if (anchor == "") continue
            if (deduplicate) {
                if (set == null) set = HashSet()
                val lcAnchor = anchor.toLowerCase()

                // Check if already processed the current anchor
                if (!set.contains(lcAnchor)) {
                    doc.add("anchor", anchor)

                    // Add to set
                    set.add(lcAnchor)
                }
            } else {
                doc.add("anchor", anchor)
            }
        }
        return doc
    }

    companion object {
        val LOG = LoggerFactory.getLogger(AnchorIndexingFilter::class.java)
    }
}