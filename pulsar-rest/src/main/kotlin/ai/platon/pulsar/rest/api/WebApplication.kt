package ai.platon.pulsar.rest.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.ImportResource
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@SpringBootApplication
@EntityScan("ai.platon.pulsar.rest.api.entities")
@EnableMongoRepositories("ai.platon.pulsar.rest.api.persist")
@ImportResource("classpath:rest-beans/app-context.xml")
class WebApplication : SpringBootServletInitializer()
