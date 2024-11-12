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
package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.crawl.fetch.FetchResult
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver

/**
 * A privacy context is a unique context of a privacy agent to the target website,
 * it will be closed once it is leaked.
 *
 * One of the biggest difficulties in web scraping tasks is the bot stealth.
 *
 * For web scraping tasks, the website should have no idea whether a visit is
 * from a human being or a bot. Once a page visit is suspected by the website,
 * which we call a privacy leak, the privacy context has to be dropped,
 * and Pulsar will visit the page in another privacy context.
 * */
interface PrivacyContext {
    val id: PrivacyAgentId
    val isIdle: Boolean
    val isRetired: Boolean
    val isLeaked: Boolean
    val isGood: Boolean
    val isActive: Boolean
    val isClosed: Boolean
    val isReady: Boolean
    fun promisedWebDriverCount(): Int
    fun hasWebDriverPromise(): Boolean
    suspend fun open(url: String): FetchResult
    suspend fun open(url: String, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult
    suspend fun open(url: String, options: LoadOptions): FetchResult
    @Throws(ProxyException::class, Exception::class)
    suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult
    @Throws(ProxyException::class)
    suspend fun doRun(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult
    fun dismiss()
    fun maintain()
}
