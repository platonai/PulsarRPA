package ai.platon.pulsar.rest.openapi

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.test.context.ContextConfiguration

/**
 * Minimal Spring Boot application for WebDriver API tests.
 * This application only loads the webdriver components without the full Pulsar context.
 */
@SpringBootApplication
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@ComponentScan(
    "ai.platon.pulsar.boot.autoconfigure",
    "ai.platon.pulsar.rest"
)
@ImportResource("classpath:rest-beans/app-context.xml")
class WebDriverTestApplication
