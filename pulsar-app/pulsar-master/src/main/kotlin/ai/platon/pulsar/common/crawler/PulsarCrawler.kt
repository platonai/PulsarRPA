package ai.platon.pulsar.common.crawler

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.StreamingCrawlLoop
import ai.platon.pulsar.crawl.common.GlobalCache
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
class PulsarCrawler(
    val globalCache: GlobalCache
) {
    private val fetchCache get() = globalCache.fetchCacheManager.normalCache

    @Bean
    fun generate() {
        val resource = "config/sites/amazon/crawl/inject/seeds/category/best-sellers/leaf-categories.txt"

        LinkExtractors.fromResource(resource)
            .map { Hyperlink(it, args = "-i 1s") }
            .toCollection(fetchCache.nReentrantQueue)
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun fetch(): StreamingCrawlLoop {
        val session: PulsarSession = PulsarContexts.createSession()
        return StreamingCrawlLoop(session, globalCache)
    }
}

fun main(args: Array<String>) {
    runApplication<PulsarCrawler>(*args) {
        setRegisterShutdownHook(true)
        setLogStartupInfo(true)
    }
}
