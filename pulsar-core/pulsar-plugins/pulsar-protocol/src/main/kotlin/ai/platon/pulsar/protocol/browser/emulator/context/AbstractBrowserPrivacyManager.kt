package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.skeleton.crawl.fetch.FetchResult
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.AbstractPrivacyManager
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyAgent
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyManager

interface BrowserPrivacyManager: PrivacyManager {
    val driverPoolManager: WebDriverPoolManager
    val proxyPoolManager: ProxyPoolManager?
}

abstract class AbstractBrowserPrivacyManager(
    override val driverPoolManager: WebDriverPoolManager,
    override val proxyPoolManager: ProxyPoolManager? = null,
    conf: ImmutableConfig
): BrowserPrivacyManager, AbstractPrivacyManager(conf)
