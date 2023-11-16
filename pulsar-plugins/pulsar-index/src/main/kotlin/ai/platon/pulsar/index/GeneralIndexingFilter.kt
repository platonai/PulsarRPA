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
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.crawl.index.IndexingException
import ai.platon.pulsar.crawl.index.IndexingFilter
import ai.platon.pulsar.persist.WebPage

/**
 * Adds basic searchable fields to a document.
 */
class GeneralIndexingFilter(
    override var conf: ImmutableConfig,
) : IndexingFilter {
    
    /**
     */
    override fun setup(conf: ImmutableConfig) {
    }
    
    /**
     * @param doc  The [IndexDocument] object
     * @param url  URL to be filtered for anchor text
     * @param page [WebPage] object relative to the URL
     * @return filtered IndexDocument
     */
    @Throws(IndexingException::class)
    override fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument? {
        return doc
    }
}
