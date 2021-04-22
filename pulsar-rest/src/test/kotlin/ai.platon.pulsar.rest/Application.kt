package ai.platon.pulsar.rest

import ai.platon.pulsar.boot.autoconfigure.pulsar.PulsarContextInitializer
import ai.platon.pulsar.boot.autoconfigure.pulsar.test.PulsarTestContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

@SpringBootApplication
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@ComponentScan(basePackages = ["ai.platon.pulsar.rest.api"])
@EntityScan("ai.platon.pulsar.rest.api.entities")
@EnableMongoRepositories("ai.platon.pulsar.rest.api.persist")
@ImportResource("classpath:rest-beans/app-context.xml")
class Application
