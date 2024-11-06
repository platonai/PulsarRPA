/**
 * Copyright (c) Vincent Zhang, ivincent.zhang@gmail.com, Platon.AI.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.skeleton.crawl.CoreMetrics
import ai.platon.pulsar.skeleton.crawl.fetch.FetchResult
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyAgent
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import com.google.common.collect.Iterables
import org.slf4j.LoggerFactory

class BasicPrivacyContextManager(
    val driverPoolManager: WebDriverPoolManager,
    val proxyPoolManager: ProxyPoolManager? = null,
    val coreMetrics: CoreMetrics? = null,
    config: ImmutableConfig
): PrivacyManager(config) {
    private val logger = LoggerFactory.getLogger(BasicPrivacyContextManager::class.java)
    private val numPrivacyContexts: Int get() = conf.getInt(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, 2)

    private val iterator = Iterables.cycle(temporaryContexts.values).iterator()

    constructor(driverPoolManager: WebDriverPoolManager, config: ImmutableConfig)
            : this(driverPoolManager, null, null, config)

    override suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        return run0(computeNextContext(task.page, task.fingerprint, task), task, fetchFun)
    }

    override fun createUnmanagedContext(privacyAgent: PrivacyAgent): BrowserPrivacyContext {
        val context = BrowserPrivacyContext(proxyPoolManager, driverPoolManager, coreMetrics, conf, privacyAgent)
        logger.info("Privacy context is created #{}", context.display)
        return context
    }
    override fun computeNextContext(fingerprint: Fingerprint): PrivacyContext {
        val task = FetchTask.create(WebPage.NIL, fingerprint)
        return computeNextContext(task.page, fingerprint, task)
    }
    override fun computeNextContext(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext {
        val context = computeIfNecessary(page, fingerprint, task)
        return context.takeIf { it.isActive } ?: run { close(context); computeIfAbsent(privacyAgentGenerator(fingerprint)) }
    }
    override fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext {
        val task = FetchTask.create(WebPage.NIL, fingerprint)
        return computeIfNecessary(task.page, fingerprint, task)
    }

    override fun computeIfNecessary(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext {
        synchronized(contextLifeCycleMonitor) {
            if (temporaryContexts.size < numPrivacyContexts) {
                val generator = privacyAgentGeneratorFactory.generator
                computeIfAbsent(generator(fingerprint))
            }

            return iterator.next()
        }
    }

    override fun computeIfAbsent(privacyAgent: PrivacyAgent) =
        temporaryContexts.computeIfAbsent(privacyAgent) { createUnmanagedContext(it) }

    private suspend fun run0(
        privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult
    ): FetchResult {
        return takeIf { isActive } ?.run1(privacyContext, task, fetchFun) ?:
        FetchResult.crawlRetry(task, "Inactive privacy context")
    }

    private suspend fun run1(privacyContext: PrivacyContext, task: FetchTask,
                             fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        if (privacyContext !is BrowserPrivacyContext) {
            throw ClassCastException("The privacy context should be a BrowserPrivacyContext")
        }

        return try {
            task.markReady()
            privacyContext.run(task) { _, driver ->
                task.startWork()
                fetchFun(task, driver)
            }
        } finally {
            task.done()
            task.page.variables["privacyContext"] = formatPrivacyContext(privacyContext)
        }
    }

    private fun formatPrivacyContext(privacyContext: PrivacyContext): String {
        return String.format("%s(%.2f)", privacyContext.privacyAgent.display, privacyContext.meterSuccesses.fiveMinuteRate)
    }
}
