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
package ai.platon.pulsar.crawl.protocol

import ai.platon.pulsar.common.config.Configurable
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.WebPage
import crawlercommons.robots.BaseRobotRules
import org.slf4j.LoggerFactory

/**
 * A retriever of url content. Implemented by protocol extensions.
 *
 * TODO: protocols are designed to be initialized at setConf() method, which is not good
 */
interface Protocol : Configurable, AutoCloseable {

    val supportParallel: Boolean

    fun setResponse(response: Response) {}

    fun getResponses(pages: Collection<WebPage>, volatileConfig: VolatileConfig): Collection<Response> {
        return emptyList()
    }

    /**
     * Reset the protocol environment, so the peer host view the client as a new one
     */
    fun reset() {}

    /**
     * Cancel the page
     */
    fun cancel(page: WebPage) {}

    /**
     * Cancel all fetching tasks
     */
    fun cancelAll() {}

    /**
     * Returns the [ProtocolOutput] for a fetch list entry.
     */
    fun getProtocolOutput(page: WebPage): ProtocolOutput

    /**
     * Returns the [ProtocolOutput] for a fetch list entry.
     */
    suspend fun getProtocolOutputDeferred(page: WebPage): ProtocolOutput

    /**
     * Retrieve robot rules applicable for this url.
     *
     * @param page The Web page
     * @return robot rules (specific for this url or default), never null
     */
    fun getRobotRules(page: WebPage): BaseRobotRules

    override fun close() {}

    companion object {
        val log = LoggerFactory.getLogger(Protocol::class.java)
    }
}
