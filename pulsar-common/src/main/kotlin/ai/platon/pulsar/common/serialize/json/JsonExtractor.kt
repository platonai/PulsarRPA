package ai.platon.pulsar.common.serialize.json

import ai.platon.pulsar.common.serialize.json.JsonExtractor.extractJsonBlocks
import java.util.function.Consumer

object JsonExtractor {

    fun extractJsonBlocks(text: String): List<String> {
        val jsonBlocks: MutableList<String> = ArrayList()
        var braceCount = 0
        var start = -1

        for (i in text.indices) {
            val ch = text[i]
            if (ch == '{') {
                if (braceCount == 0) {
                    start = i
                }
                braceCount++
            } else if (ch == '}') {
                braceCount--
                if (braceCount == 0 && start != -1) {
                    jsonBlocks.add(text.substring(start, i + 1))
                    start = -1
                }
            }
        }

        return jsonBlocks
    }
}

fun main(args: Array<String>) {
    val text = "Here is something before JSON... {\"name\":\"Vincent\",\"role\":\"Dev\"} and some after."
    val blocks = extractJsonBlocks(text)
    blocks.forEach(Consumer { x: String? -> println(x) })
}
