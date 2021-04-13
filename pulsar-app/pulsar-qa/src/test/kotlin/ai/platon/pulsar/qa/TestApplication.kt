package ai.platon.pulsar.qa

import ai.platon.pulsar.boot.autoconfigure.pulsar.PulsarContextInitializer
import ai.platon.pulsar.common.options.LoadOptionDefaults
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.test.context.ContextConfiguration

@SpringBootApplication
@ComponentScan("ai.platon.pulsar.boot.autoconfigure.pulsar")
@ImportResource("classpath:test-beans/app-context.xml")
@ContextConfiguration(initializers = [PulsarContextInitializer::class])
class TestApplication
