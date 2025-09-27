package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserFactory
import kotlinx.coroutines.runBlocking

class InstructionsExample {
    fun run() {
        PulsarSettings().withSPA()
        val session = PulsarContexts.getOrCreateSession()
        val browserFactory = session.context.getBean(BrowserFactory::class)
        val driver = browserFactory.launchDefaultBrowser().newDriver()

        runBlocking {
            driver.navigateTo("https://docs.browserbase.com/")
            driver.act("secret12345")
            driver.act("search for 'Calabi-Yau manifold'")
        }
    }
}

fun main() {
    InstructionsExample().run()
}
