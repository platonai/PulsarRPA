package ai.platon.pulsar.app.crawler

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.crawl.StreamingCrawlLoop
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
class PulsarCrawler(
    val globalCacheFactory: GlobalCacheFactory
) {
    val globalCache get() = globalCacheFactory.globalCache

    private val fetchCache get() = globalCache.fetchCaches.normalCache

    @Autowired
    lateinit var unmodifiedConfig: ImmutableConfig

    @Bean
    fun generate() {
        val resource = "config/sites/amazon/crawl/inject/seeds/category/best-sellers/leaf-categories.txt"

        LinkExtractors.fromResource(resource)
            .map { Hyperlink(it, args = "-i 1s") }
            .toCollection(fetchCache.nReentrantQueue)
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun fetch(): StreamingCrawlLoop {
        return StreamingCrawlLoop(globalCacheFactory, unmodifiedConfig)
    }
}

fun main(args: Array<String>) {
    runApplication<PulsarCrawler>(*args) {
        setRegisterShutdownHook(true)
        setLogStartupInfo(true)
    }
}
