package ai.platon.pulsar.rest.api

import ai.platon.pulsar.skeleton.crawl.CrawlLoops
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:rest-beans/app-context.xml")
@EntityScan("ai.platon.pulsar.rest.api.entities")
@ComponentScan(
    "ai.platon.pulsar.boot.autoconfigure",
    "ai.platon.pulsar.rest.api"
)
class ApiApplication(
    /**
     * Activate crawl loops
     * */
    val crawlLoops: CrawlLoops
)
