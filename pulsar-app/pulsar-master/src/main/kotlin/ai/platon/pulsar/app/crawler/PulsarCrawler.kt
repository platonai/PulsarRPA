package ai.platon.pulsar.app.crawler

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
class PulsarCrawler(
    private val globalCacheFactory: GlobalCacheFactory
) {
    private val globalCache get() = globalCacheFactory.globalCache
    private val urlCache get() = globalCache.urlPool.normalCache

    @Bean
    fun generate() {
        val resource = "seeds/amazon/best-sellers/leaf-categories.txt"
        LinkExtractors.fromResource(resource).mapTo(urlCache.nReentrantQueue) { Hyperlink(it, args = "-i 1s") }
    }
}

fun main(args: Array<String>) {
    runApplication<PulsarCrawler>(*args) {
        setRegisterShutdownHook(true)
        setLogStartupInfo(true)
    }
}
