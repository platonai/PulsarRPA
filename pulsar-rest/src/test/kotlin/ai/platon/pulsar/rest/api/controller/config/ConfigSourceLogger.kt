package ai.platon.pulsar.rest.api.controller.config

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
class ConfigSourceLogger(private val env: ConfigurableEnvironment) {

    @EventListener(ApplicationReadyEvent::class)
    fun logPropertySources() {
        println(Paths.get(".").toAbsolutePath())

        println("=== Spring Boot Property Sources ===")
        env.propertySources.forEach { ps ->
            println("Source: ${ps.name} ${ps.source}")
        }
    }
}
