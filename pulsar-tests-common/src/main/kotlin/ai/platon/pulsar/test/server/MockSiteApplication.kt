package ai.platon.pulsar.test.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.pulsar.test.server",
    ]
)
class MockSiteApplication

fun main() {
    runApplication<MockSiteApplication>()
}
