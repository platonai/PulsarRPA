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
package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.PulsarEnv.Companion.getBean
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.BrowserEmulatedFetcher
import ai.platon.pulsar.protocol.crowd.ForwardingProtocol
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import java.util.concurrent.atomic.AtomicReference

class BrowserEmulatorProtocol : ForwardingProtocol() {
    private val log = LoggerFactory.getLogger(BrowserEmulatorProtocol::class.java)
    private val browserEmulator = AtomicReference<BrowserEmulatedFetcher?>()
    /**
     * Called just after creation
     * @see ai.platon.pulsar.crawl.protocol.ProtocolFactory.ProtocolFactory
     *
     */
    override fun setConf(jobConf: ImmutableConfig) {
        super.setConf(jobConf)
    }

    override fun supportParallel(): Boolean {
        return true
    }

    override fun getResponses(pages: Collection<WebPage>, volatileConfig: VolatileConfig): Collection<Response> {
        try {
            return fetcher?.parallelFetchAllPages(pages, volatileConfig)?: emptyList()
        } catch (e: Exception) {
            log.warn("Unexpected exception", e)
        }
        return emptyList()
    }

    public override fun getResponse(url: String, page: WebPage, followRedirects: Boolean): Response {
        return try {
            val response = super.getResponse(url, page, followRedirects)
            if (response != null) {
                return response
            }
            return fetcher?.fetchContent(page)?:ForwardingResponse.canceled(page)
        } catch (e: Exception) {
            log.warn("Unexpected exception", e)
            // Unexpected exception, cancel the request, hope to retry in CRAWL_SOLUTION scope
            ForwardingResponse.canceled(page)
        }
    }

    override fun reset() {
        val fetcher = fetcher
        if (fetcher != null) {
            // fetcher.privacyContext.reset();
        }
    }

    override fun cancel(page: WebPage) {
        val fetcher = fetcher
        if (fetcher != null) {
            // fetcher.privacyContext.cancel(page);
        }
    }

    override fun cancelAll() {
        val fetcher = fetcher
        if (fetcher != null) {
            // fetcher.privacyContext.cancelAll();
        }
    }

    override fun close() {
        closed.set(true)
    }

    @get:Synchronized
    private val fetcher: BrowserEmulatedFetcher?
        get() {
            if (closed.get()) {
                return null
            }
            try {
                browserEmulator.compareAndSet(null, getBean(BrowserEmulatedFetcher::class.java))
            } catch (e: BeansException) {
                log.warn("{}", Strings.simplifyException(e))
            }
            return browserEmulator.get()
        }
}
