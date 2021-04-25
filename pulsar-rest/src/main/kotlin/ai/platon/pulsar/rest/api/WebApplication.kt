package ai.platon.pulsar.rest.api

import ai.platon.pulsar.boot.autoconfigure.pulsar.PulsarContextInitializer
import ai.platon.pulsar.boot.autoconfigure.pulsar.test.PulsarTestContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportResource
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@SpringBootApplication
@Configuration
@ImportResource("classpath:rest-beans/app-context.xml")
@EntityScan("ai.platon.pulsar.rest.api.entities")
@ComponentScan(
    "ai.platon.pulsar.boot.autoconfigure.pulsar",
    "ai.platon.pulsar.rest.api"
)
class WebApplication : SpringBootServletInitializer()
