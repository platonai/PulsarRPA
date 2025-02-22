package ai.platon.pulsar.app.crawler

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.skeleton.context.PulsarContexts
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
class PulsarCrawler {
    private val session = PulsarContexts.createSession()

    @PostConstruct
    fun generate() {
        val resource = "seeds/amazon/best-sellers/leaf-categories.txt"
        LinkExtractors.fromResource(resource).asSequence()
            .map { Hyperlink(it, "", args = "-i 1s") }
            .forEach { session.submit(it) }
    }
}

fun main(args: Array<String>) {
    runApplication<PulsarCrawler>(*args)
}
