package ai.platon.pulsar.app.master

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.boot.autoconfigure.pulsar.PulsarContextInitializer
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.StreamingCrawlStarter
import ai.platon.pulsar.crawl.common.GlobalCache
import org.h2.tools.Server
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import java.sql.SQLException

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan("ai.platon.pulsar.rest.api")
class PulsarMaster(
    val globalCache: GlobalCache
) {
    private val log = LoggerFactory.getLogger(PulsarMaster::class.java)
    private val fetchCache get() = globalCache.fetchCacheManager.normalCache
    @Autowired
    lateinit var unmodifiedConfig: ImmutableConfig

    @Bean
    fun commandLineRunner(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner { args ->
            val beans = ctx.beanDefinitionNames.sorted()
            val s = beans.joinToString("\n") { it }
            val path = AppPaths.getTmp("spring-beans.txt")
            AppFiles.saveTo(s, path)
            log.info("Report of all active spring beans is written to $path")
        }
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @Throws(SQLException::class)
    fun h2Server(): Server {
        // return Server.createTcpServer("-trace")
        return Server.createTcpServer()
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @Throws(SQLException::class)
    fun h2WebServer(): Server {
        return Server.createWebServer("-webAllowOthers")
    }

    @Bean
    fun generate() {
        val resource = "config/sites/amazon/crawl/inject/seeds/category/best-sellers/leaf-categories.txt"

        LinkExtractors.fromResource(resource)
            .map { Hyperlink(it, args = "-i 1s") }
            .toCollection(fetchCache.nReentrantQueue)
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun fetch(): StreamingCrawlStarter {
        return StreamingCrawlStarter(globalCache, unmodifiedConfig)
    }
}

fun main(args: Array<String>) {
    runApplication<PulsarMaster>(*args) {
        // setAdditionalProfiles("rest", "crawler", "amazon")
        addInitializers(PulsarContextInitializer())
        setRegisterShutdownHook(true)
        setLogStartupInfo(true)
    }
}
