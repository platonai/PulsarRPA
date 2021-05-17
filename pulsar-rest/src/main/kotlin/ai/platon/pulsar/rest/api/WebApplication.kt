package ai.platon.pulsar.rest.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@Configuration
@ImportResource("classpath:rest-beans/app-context.xml")
@EntityScan("ai.platon.pulsar.rest.api.entities")
@ComponentScan(
    "ai.platon.pulsar.boot.autoconfigure.pulsar",
    "ai.platon.pulsar.rest.api"
)
class WebApplication : SpringBootServletInitializer()
