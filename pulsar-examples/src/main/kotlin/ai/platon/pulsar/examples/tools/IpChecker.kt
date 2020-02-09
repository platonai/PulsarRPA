package ai.platon.pulsar.examples.tools

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import org.slf4j.LoggerFactory

class IpChecker: AutoCloseable {
    private val pc = PulsarContext.getOrCreate()
    private val i = pc.createSession()
    private val log = LoggerFactory.getLogger(IpChecker::class.java)

    private val onlineCheckTools = listOf(
//            "https://httpbin.org/headers",
            "https://whatleaks.com/"
//            "https://ipleak.net/"
    )

    fun check() {
        onlineCheckTools.forEach {
            check(it)
        }
    }

    fun check(url: String) {
        val options = LoadOptions.parse("-i 1s")
        val page = i.load(url, options)
        val doc = i.parse(page)
        doc.absoluteLinks()
        val path = i.export(doc)
        log.info("Export to: file://{}", path)

        readLine()
    }

    override fun close() {
        i.close()
        pc.close()
        pc.env.shutdown()
    }
}

fun main() {
    System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

    IpChecker().use { it.check() }
}
