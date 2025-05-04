package ai.platon.pulsar.test.rest

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportResource
import org.springframework.test.context.ContextConfiguration

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.pulsar.boot.autoconfigure",
        "ai.platon.pulsar.test.rest"
    ]
)
@ImportResource("classpath:pulsar-beans/test-app-context.xml")
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class Application

fun main() {
    runApplication<Application>()
}
