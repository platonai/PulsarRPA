package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.crawl.fetch.FetchResult
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.WebDriverFetcher
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver

interface PrivacyManager : AutoCloseable {
    val isActive: Boolean
    val isClosed: Boolean
    val conf: ImmutableConfig
    
    fun buildStatusString(): String
    fun maintain(force: Boolean = false)
    fun reset(reason: String = "")
    
    suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult
    fun tryGetNextReadyPrivacyContext(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext
    fun tryGetNextReadyPrivacyContext(fingerprint: Fingerprint): PrivacyContext
    fun tryGetNextUnderLoadedPrivacyContext(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext?
    fun getOrCreate(privacyAgent: PrivacyAgent): PrivacyContext
    fun createUnmanagedContext(privacyAgent: PrivacyAgent): PrivacyContext
    fun createUnmanagedContext(privacyAgent: PrivacyAgent, fetcher: WebDriverFetcher): PrivacyContext
    fun close(privacyContext: PrivacyContext)
}