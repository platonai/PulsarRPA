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
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.WebPageFormatter
import ai.platon.pulsar.persist.gora.db.DbQuery
import ai.platon.pulsar.persist.gora.db.DbQueryResult
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The WebDb component.
 */
class WebDbComponent(private val webDb: WebDb, private val conf: ImmutableConfig) : AutoCloseable {
    private val isClosed = AtomicBoolean()

    constructor(conf: ImmutableConfig) : this(WebDb(conf), conf) {}

    fun put(url: String, page: WebPage) {
        webDb.put(page)
    }

    fun put(page: WebPage) {
        webDb.put(page)
    }

    fun flush() {
        webDb.flush()
    }

    operator fun get(url: String): WebPage {
        return webDb.get(url)
    }

    fun delete(url: String): Boolean {
        return webDb.delete(url)
    }

    fun truncate(): Boolean {
        return webDb.truncate()
    }

    fun scan(startUrl: String, endUrl: String): DbQueryResult {
        val result = DbQueryResult()
        val crawlId = conf[CapabilityTypes.STORAGE_CRAWL_ID] ?: ""
        val query = DbQuery(crawlId, AppConstants.ALL_BATCHES, startUrl, endUrl)
        Params.of("startUrl", startUrl, "endUrl", endUrl).withLogger(LOG).debug(true)
        val iterator = webDb.query(query)
        while (iterator.hasNext()) {
            result.addValue(WebPageFormatter(iterator.next()).toMap(query.fields))
        }
        return result
    }

    fun query(query: DbQuery): DbQueryResult {
        val result = DbQueryResult()
        val iterator = webDb.query(query)
        while (iterator.hasNext()) {
            result.addValue(WebPageFormatter(iterator.next()).toMap(query.fields))
        }
        return result
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }
        webDb.close()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(WebDbComponent::class.java)
    }

}
