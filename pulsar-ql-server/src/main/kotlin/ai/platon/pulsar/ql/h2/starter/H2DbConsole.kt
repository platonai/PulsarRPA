package ai.platon.pulsar.ql.h2.starter

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.context.support.GenericPulsarContext
import org.h2.tools.Server
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.boot.context.event.ApplicationPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import java.sql.SQLException

/**
 * Server start port
 */
@SpringBootApplication(exclude = [MongoAutoConfiguration::class, EmbeddedMongoAutoConfiguration::class])
@ImportResource("classpath:pulsar-beans/app-context.xml")
class H2DbConsole {

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
}

fun main(args: Array<String>) {
    val application = SpringApplication(H2DbConsole::class.java)
    val event = ApplicationListener<ApplicationPreparedEvent> {
        PulsarContexts.activate(GenericPulsarContext(it.applicationContext)) }
    application.addListeners(event)
    application.run(*args)
}
