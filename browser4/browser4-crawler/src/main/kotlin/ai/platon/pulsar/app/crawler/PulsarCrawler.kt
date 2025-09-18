package ai.platon.pulsar.app.crawler

import ai.platon.pulsar.boot.autoconfigure.PulsarContextConfiguration
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.persist.HadoopConfiguration
import ai.platon.pulsar.persist.HadoopUtils
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.session.PulsarSession
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@Import(PulsarContextConfiguration::class)
class PulsarCrawler(
    val session: PulsarSession
) {
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
