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

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserFiles
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.crawl.fetch.FetchResult
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import java.nio.file.Path
import java.time.Duration

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
interface PrivacyContext: AutoCloseable {
    val failureRate: Float
    val isHighFailureRate: Boolean
    val idleTime: Duration
    val elapsedTime: Duration
    val isFullCapacity: Boolean
    val isUnderLoaded: Boolean
    val id: PrivacyAgentId
    val isIdle: Boolean
    val isRetired: Boolean
    val isLeaked: Boolean
    val isGood: Boolean
    val isActive: Boolean
    val isClosed: Boolean
    val isReady: Boolean
    val display: String
    val readableState: String
    val privacyAgent: PrivacyAgent
    fun buildStatusString(): String
    fun promisedWebDriverCount(): Int
    fun hasWebDriverPromise(): Boolean
    suspend fun open(url: String): FetchResult
    suspend fun open(url: String, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult
    suspend fun open(url: String, options: LoadOptions): FetchResult
    suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult
    suspend fun doRun(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult
    fun dismiss()
    fun maintain()
    fun buildReport(): String
    
    companion object {
        // The prefix for all temporary privacy contexts. System context, prototype context and default context are not
        // required to start with the prefix.
        const val CONTEXT_DIR_PREFIX = "cx."
        
        // The default context directory, if you need a permanent and isolate context, use this one.
        // NOTE: the user-default context is not a default context.
        val DEFAULT_CONTEXT_DIR: Path = AppPaths.CONTEXT_DEFAULT_DIR
        // A random context directory, if you need a random temporary context, use this one
        val NEXT_SEQUENTIAL_CONTEXT_DIR get() = BrowserFiles.computeNextSequentialContextDir()
        // A random context directory, if you need a random temporary context, use this one
        val RANDOM_CONTEXT_DIR get() = BrowserFiles.computeRandomTmpContextDir()
        // The prototype context directory, all privacy contexts copies browser data from the prototype.
        // A typical prototype data dir is: ~/.pulsar/browser/chrome/prototype/google-chrome/
        val PROTOTYPE_DATA_DIR: Path = AppPaths.CHROME_DATA_DIR_PROTOTYPE
        // A context dir is the dir which contains the browser data dir, and supports different browsers.
        // For example: ~/.pulsar/browser/chrome/prototype/
        val PROTOTYPE_CONTEXT_DIR: Path = AppPaths.CHROME_DATA_DIR_PROTOTYPE.parent
    }
}
