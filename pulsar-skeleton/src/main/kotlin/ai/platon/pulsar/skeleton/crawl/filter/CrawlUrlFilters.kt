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
package ai.platon.pulsar.skeleton.crawl.filter

import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory

/**
 * Creates and caches [CrawlUrlFilter] implementing plugins.
 */
class CrawlUrlFilters(
    val urlFilters: List<CrawlUrlFilter>,
    val conf: ImmutableConfig
) {
    val LOG = LoggerFactory.getLogger(CrawlUrlFilters::class.java)

    constructor(conf: ImmutableConfig): this(listOf(), conf)

    /**
     * Run all defined urlFilters. Assume logical AND.
     */
    fun filter(url: String): String? {
        var u: String? = url
        for (urlFilter in urlFilters) {
            if (u != null) {
                u = urlFilter.filter(u)
                if (u == null) {
                    LOG.debug("Url is deleted by {} | {}", urlFilter.javaClass.simpleName, url)
                    break
                }
            }
        }
        return u
    }

    override fun toString(): String {
        return urlFilters.joinToString { it.javaClass.simpleName }
    }
}