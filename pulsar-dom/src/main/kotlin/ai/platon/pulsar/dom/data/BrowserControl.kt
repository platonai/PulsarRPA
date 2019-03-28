package ai.platon.pulsar.dom.data

import ai.platon.pulsar.common.ResourceLoader
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.io.IOException
import java.util.*

class BrowserControl(private val conf: ai.platon.pulsar.common.config.ImmutableConfig) {
    companion object {
        val log = LoggerFactory.getLogger(BrowserControl::class.java)!!
        val viewPort: Dimension = Dimension(1920, 1080)

        private var loaded = false
        private lateinit var js: String
    }

    init {
        if (!loaded) {
            js = loadJs()
            loaded = true
        }
    }

    fun getJs(): String {
        return js
    }

    private fun loadJs(): String {
        val sb = StringBuilder()
        Arrays.asList(
                "js/__utils__.js",
                "js/node_traversor.js",
                "js/node_visitor.js",
                "js/humanize.js"
        ).forEach { resource ->
            val reader = ResourceLoader().getResourceAsStream(resource)
            try {
                val s = IOUtils.readLines(reader).joinToString("\n")
                sb.append(s).append(";\n")
            } catch (e: IOException) {
                log.error(e.toString())
            }
        }

        sb.append(";\n")
                .append("__utils__.scrollToBottom();\n")
                .append("__utils__.scrollToTop();\n")
                .append("__utils__.visualizeHumanize();\n")
                .append(";\n")

        // init view port for js
        val js = sb.toString()
                .replace("{VIEW_PORT_WIDTH}", viewPort.width.toString())
                .replace("{VIEW_PORT_HEIGHT}", viewPort.height.toString())

        return js
    }
}
