package ai.platon.pulsar.dom.data

import ai.platon.pulsar.common.ResourceLoader
import com.google.gson.GsonBuilder
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.io.IOException
import java.util.*

class BrowserControl(parameters: Map<String, Any> = mapOf()) {
    companion object {
        val log = LoggerFactory.getLogger(BrowserControl::class.java)!!
        val viewPort = Dimension(1920, 1080)
    }

    private val jsParameters = mutableMapOf<String, Any>()
    private var js: String = ""

    init {
        mapOf(
                "viewPortWidth" to viewPort.width,
                "viewPortHeight" to viewPort.height
        ).also { jsParameters.putAll(it) }

        jsParameters.putAll(parameters)
    }

    fun getJs(reload: Boolean = false): String {
        if (reload || js.isEmpty()) {
            js = loadJs()
        }

        return js
    }

    private fun loadJs(): String {
        val sb = StringBuilder()

        // Note: Json-2.6.2 does not recognize MutableMap, but knows Map
        val configs = GsonBuilder().create().toJson(jsParameters.toMap())
        sb.appendln(";\nlet PULSAR_CONFIGS = $configs;")

        Arrays.asList(
                "js/__utils__.js",
                "js/humanize.js",
                "js/node_traversor.js",
                "js/node_visitor.js"
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
                .append("__utils__.scrollToTop();\n")
                .append("__utils__.visualizeHumanize();\n")
                .append(";\n")

        return sb.toString()
    }
}
