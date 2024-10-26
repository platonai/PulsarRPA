package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.skeleton.crawl.CrawlLoops
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@SpringBootApplication
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@ComponentScan(basePackages = ["ai.platon.pulsar.rest.api"])
@EntityScan("ai.platon.pulsar.rest.api.entities")
@ImportResource("classpath:rest-beans/app-context.xml")
class Application {
    @Bean
    fun restTemplate(): TestRestTemplate {
        return TestRestTemplate(RestTemplateBuilder().setReadTimeout(Duration.ofMinutes(2)))
    }
}
