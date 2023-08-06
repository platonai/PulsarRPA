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
package ai.platon.pulsar.indexer.solr

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlUtils.reverseUrlOrEmpty
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.crawl.index.IndexingFilters
import ai.platon.pulsar.persist.WebPageExt.Companion.newTestWebPage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/index-beans.xml"])
class TestIndexingFilters {
    private val indexingFiltersClassNames = Strings.humanize(IndexingFilters::class.java, "classes", ".")
    
    @Autowired
    private lateinit var conf: ImmutableConfig
    
    @Autowired
    private lateinit var filters: IndexingFilters

    /**
     * Test behaviour when defined filter does not exist.
     */
    @Test
    fun testNonExistingIndexingFilter() {
        val class1 = "NonExistingFilter"
        val class2 = "org.apache.pulsar.indexer.basic.BasicIndexingFilter"
        conf.unbox()[indexingFiltersClassNames] = "$class1,$class2"
        val url = "https://www.example.com/"
        val page = newTestWebPage(url)
        page.pageText = "text"
        page.pageTitle = "title"
        val key = reverseUrlOrEmpty(url)
        filters.filter(IndexDocument(key), url, page)
    }
}
