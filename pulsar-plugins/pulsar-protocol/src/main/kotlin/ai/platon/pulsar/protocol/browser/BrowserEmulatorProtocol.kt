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

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.BrowserEmulatedFetcher
import ai.platon.pulsar.protocol.browser.emulator.Defaults
import ai.platon.pulsar.protocol.crowd.ForwardingProtocol

class BrowserEmulatorProtocol : ForwardingProtocol() {
    private val context get() = PulsarContexts.create()

    private val browserEmulator by lazy {
        // require(conf === context.unmodifiedConfig)
        context.getBeanOrNull(BrowserEmulatedFetcher::class)
            ?: Defaults(conf).browserEmulatedFetcher.also { context.registerClosable(it) }
    }

    override fun getResponse(page: WebPage, followRedirects: Boolean): Response? {
        require(page.isNotInternal) { "Unexpected internal page ${page.url}" }
        return super.getResponse(page, followRedirects) ?: browserEmulator.fetchContent(page)
    }

    override suspend fun getResponseDeferred(page: WebPage, followRedirects: Boolean): Response? {
        require(page.isNotInternal) { "Unexpected internal page ${page.url}" }
        return super.getResponse(page, followRedirects) ?: browserEmulator.fetchContentDeferred(page)
    }

    override fun reset() {
        browserEmulator.reset()
    }

    override fun cancel(page: WebPage) {
        browserEmulator.cancel(page)
    }

    override fun cancelAll() {
        browserEmulator.cancelAll()
    }
}
