package ai.platon.pulsar.app

import ai.platon.pulsar.boot.autoconfigure.PulsarContextInitializer
import org.h2.tools.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import java.sql.SQLException

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan("ai.platon.pulsar.rest.api")
class PulsarApplication {
    /**
     * Enable H2 client
     * */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @Throws(SQLException::class)
    fun h2Server(): Server {
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
    runApplication<PulsarApplication>(*args) {
        addInitializers(PulsarContextInitializer())
        setAdditionalProfiles("master", "private")
        setLogStartupInfo(true)
    }
}
