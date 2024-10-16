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
import ai.platon.pulsar.skeleton.crawl.index.IndexDocument
import ai.platon.pulsar.skeleton.crawl.index.IndexingException
import ai.platon.pulsar.skeleton.crawl.index.IndexingFilter
import ai.platon.pulsar.persist.WebPage
import java.util.*

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
    }
    
    @Throws(IndexingException::class)
    override fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument? {
        return doc
    }
}
