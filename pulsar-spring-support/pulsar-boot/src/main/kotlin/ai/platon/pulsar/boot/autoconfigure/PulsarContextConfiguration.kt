package ai.platon.pulsar.boot.autoconfigure

import ai.platon.pulsar.ql.context.H2SQLContext
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.skeleton.crawl.CrawlLoop
import ai.platon.pulsar.skeleton.crawl.CrawlLoops
import ai.platon.pulsar.skeleton.crawl.common.GlobalCache
import ai.platon.pulsar.skeleton.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.skeleton.session.PulsarSession
import jakarta.annotation.PostConstruct
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportResource
import org.springframework.context.annotation.Scope

@Configuration
@ImportResource("classpath:pulsar-beans/app-context.xml")
class PulsarContextConfiguration(
    val applicationContext: ApplicationContext,
    val globalCache: GlobalCache,
    val globalCacheFactory: GlobalCacheFactory,
    val crawlLoops: CrawlLoops
) {
    @Bean
    @Scope("prototype")
    fun getPulsarSession(): PulsarSession {
        val context = SQLContexts.create(applicationContext)
        require(context is H2SQLContext)
        require(context.applicationContext == applicationContext)
        return context.createSession()
    }
}
