package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools

object DomServiceFactory {
    private const val IMPL_KEY = "pulsar.dom.impl"

    fun create(devTools: RemoteDevTools): DomService {
        return when (System.getProperty(IMPL_KEY, "kotlin").lowercase()) {
            "kotlin" -> ChromeCdpDomService(devTools)
            "python" -> throw UnsupportedOperationException("Python DomService bridge not wired yet. Set -D$IMPL_KEY=kotlin to use Kotlin implementation.")
            else -> ChromeCdpDomService(devTools)
        }
    }
}
