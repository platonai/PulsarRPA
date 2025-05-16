package ai.platon.pulsar.app

import ai.platon.pulsar.boot.autoconfigure.PulsarContextInitializer
import jakarta.annotation.PostConstruct
import org.h2.tools.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import java.nio.file.Paths
import java.sql.SQLException

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan("ai.platon.pulsar.rest.api")
class PulsarApplication

fun main(args: Array<String>) {
    runApplication<PulsarApplication>(*args) {
        addInitializers(PulsarContextInitializer())
        setAdditionalProfiles("master", "private")
        setLogStartupInfo(true)
    }
}
