package ai.platon.pulsar.examples.tools

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import org.slf4j.LoggerFactory

class WhatLeaks: AutoCloseable {
    private val i = PulsarContext.getOrCreate().createSession()
    private val log = LoggerFactory.getLogger(WhatLeaks::class.java)

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
        i.context.close()
    }
}

fun main() {
    System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

    WhatLeaks().use { it.check() }
}
