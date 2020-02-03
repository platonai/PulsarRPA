package ai.platon.pulsar.browser.driver.chrome.examples

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kklisura.cdt.protocol.events.tracing.DataCollected
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class TracingExample: BrowserExampleBase() {

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
            println("Tracing completed! Dumping to $path")
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
