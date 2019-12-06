package ai.platon.pulsar.examples

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.*
import org.slf4j.LoggerFactory

object WebTester {
    private val env = PulsarEnv.initialize()
    private val pc = PulsarContext.getOrCreate()
    private val i = pc.createSession()
    private val log = LoggerFactory.getLogger(WebAccess::class.java)

    fun load() {
        val url = "https://www.finishline.com/store/men/shoes/_/N-1737dkj?mnid=men_shoes"
        val args = " -i 1s"
        val page = i.load("$url $args")
        val doc = i.parse(page)
        doc.absoluteLinks()
        doc.stripScripts()

        doc.select("a") { it.attr("abs:href") }.asSequence()
                .filter { Urls.isValidUrl(it) }
                .take(10)
                .joinToString("\n") { it }
                .also { println(it) }

        val path = i.export(doc)
        log.info("Export to: file://{}", path)
    }
}

fun main() {
    WebTester.load()

    PulsarEnv.initialize().shutdown()
}
