package ai.platon.pulsar.app.master

import ai.platon.pulsar.boot.autoconfigure.PulsarContextInitializer
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.crawl.StreamingCrawlLoop
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
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
    val globalCacheFactory: GlobalCacheFactory
) {
    private val log = LoggerFactory.getLogger(PulsarMaster::class.java)
    private val globalCache get() = globalCacheFactory.globalCache
    private val fetchCache get() = globalCache.fetchCaches.normalCache
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

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun fetch(): StreamingCrawlLoop {
        return StreamingCrawlLoop(globalCacheFactory, unmodifiedConfig)
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
