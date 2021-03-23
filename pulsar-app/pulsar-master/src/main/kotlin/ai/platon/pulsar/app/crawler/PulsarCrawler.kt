package ai.platon.pulsar.app.crawler

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.StreamingCrawlLoop
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.persist.WebDb
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
//@ComponentScan("ai.platon.pulsar.boot.autoconfigure.pulsar")
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
        println(".............................")
        println(fetchCache.nReentrantQueue.size)
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun fetch(): StreamingCrawlLoop {
        val session: PulsarSession = PulsarContexts.createSession()
        return StreamingCrawlLoop(session, globalCache)
    }
}

fun main(args: Array<String>) {
    runApplication<PulsarCrawler>(*args) {
        // setAdditionalProfiles("rest", "crawler", "amazon")
//        addInitializers(PulsarContextInitializer())
        setRegisterShutdownHook(true)
        setLogStartupInfo(true)
    }
}
