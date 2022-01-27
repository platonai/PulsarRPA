package ai.platon.pulsar.app.master

import ai.platon.pulsar.boot.autoconfigure.PulsarContextInitializer
import ai.platon.pulsar.crawl.CrawlLoops
import org.h2.tools.Server
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import java.sql.SQLException

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan("ai.platon.pulsar.rest.api")
class PulsarMaster(
    /**
     * Activate crawl loops
     * */
    val crawlLoops: CrawlLoops
) {
    private val logger = LoggerFactory.getLogger(PulsarMaster::class.java)

    /**
     * Enable H2 client
     * */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @Throws(SQLException::class)
    fun h2Server(): Server {
        // return Server.createTcpServer("-trace")
        return Server.createTcpServer()
    }

    /**
     * Enable H2 console
     * */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @Throws(SQLException::class)
    fun h2WebServer(): Server {
        return Server.createWebServer("-webAllowOthers")
    }
}

fun main(args: Array<String>) {
    runApplication<PulsarMaster>(*args) {
        addInitializers(PulsarContextInitializer())
        setAdditionalProfiles("master")
        setRegisterShutdownHook(true)
        setLogStartupInfo(true)
    }
}
