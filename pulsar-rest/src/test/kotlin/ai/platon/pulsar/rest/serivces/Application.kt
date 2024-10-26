package ai.platon.pulsar.rest.serivces

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.skeleton.crawl.CrawlLoops
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.test.context.ContextConfiguration

@SpringBootApplication
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@ComponentScan(basePackages = ["ai.platon.pulsar.rest.api"])
@EntityScan("ai.platon.pulsar.rest.api.entities")
@ImportResource("classpath:rest-beans/app-context.xml")
class Application
