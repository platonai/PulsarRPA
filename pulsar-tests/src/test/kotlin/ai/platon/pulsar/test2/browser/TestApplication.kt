package ai.platon.pulsar.test2.browser

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ImportResource

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.pulsar.boot.autoconfigure",
        "ai.platon.pulsar.test.rest"
    ]
)
@ImportResource("test-beans/app-context.xml")
class TestApplication
