package ai.platon.pulsar.browser.driver.examples

import ai.platon.pulsar.common.printlnPro
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kklisura.cdt.protocol.v2023.events.tracing.DataCollected
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class TracingExample: BrowserExampleBase() {

    override val testUrl: String = "https://www.stbchina.cn/"

    override fun run() {
        val page = devTools.page
        val tracing = devTools.tracing

        val dataCollectedList = mutableListOf<Any>()

        // Add tracing data to dataCollectedList
        tracing.onDataCollected { event: DataCollected ->
            if (event.value != null) {
                dataCollectedList.addAll(event.value)
            }
        }

        // When tracing is complete, dump dataCollectedList to JSON file.
        tracing.onTracingComplete {
            // Dump tracing to file.
            val path = Paths.get("/tmp/tracing.json")
            printlnPro("Tracing completed! Dumping to $path")
            dump(path, dataCollectedList)
            devTools.close()
        }

        page.onLoadEventFired { tracing.end() }

        page.enable()
        tracing.start()
        page.navigate(testUrl)
    }

    private fun dump(path: Path, data: List<Any>) {
        val om = ObjectMapper()
        try {
            om.writeValue(path.toFile(), data)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun main() {
    TracingExample().use { it.run() }
}
