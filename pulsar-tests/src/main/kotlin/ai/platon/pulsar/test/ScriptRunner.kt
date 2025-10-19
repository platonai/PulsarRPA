package ai.platon.pulsar.test


import ai.platon.pulsar.common.code.ProjectUtils
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.context.SQLContexts
import java.nio.file.Files
import kotlin.io.path.readText

fun main() {
    val root = ProjectUtils.findProjectRootDir()!!

    Files.walk(root)
        .filter { it.toString().contains("test") }
        .filter { it.fileName.toString().endsWith(".kt") }
        .forEach { path ->
            val content = path.readText()
            if (content.contains("logPrintln")) {
                val lines = content.split("\n")
                    .toMutableList()

                var pos = -1
                lines.forEachIndexed { index, line ->
                    if (line.startsWith("import ai.")) {
                        pos = index
                        return@forEachIndexed
                    }
                }

                if (pos > 0) {
                    Files.write(path, lines)
                }
            }
        }
}

