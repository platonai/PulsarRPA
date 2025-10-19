package ai.platon.pulsar.app.api.controller.config

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
class ConfigSourceLogger(private val env: ConfigurableEnvironment) {

    @EventListener(ApplicationReadyEvent::class)
    fun logPropertySources() {
        logPrintln(Paths.get(".").toAbsolutePath())

        logPrintln("=== Spring Boot Property Sources ===")
        env.propertySources.forEach { ps ->
            logPrintln("Source: ${ps.name} ${ps.source}")
        }
    }
}
