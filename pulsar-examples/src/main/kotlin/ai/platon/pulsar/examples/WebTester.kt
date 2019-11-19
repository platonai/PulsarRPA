package ai.platon.pulsar.examples

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_AFTER_FETCH_BATCH_HANDLER
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_BEFORE_FETCH_BATCH_HANDLER
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.component.BatchHandler
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageFormatter
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import org.slf4j.LoggerFactory
import java.net.URL

object WebTester {
    private val env = PulsarEnv.getOrCreate()
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

    PulsarEnv.getOrCreate().shutdown()
}
