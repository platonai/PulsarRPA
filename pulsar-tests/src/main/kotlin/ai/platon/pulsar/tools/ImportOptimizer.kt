package ai.platon.pulsar.tools

import ai.platon.pulsar.common.code.ProjectUtils
import java.nio.file.Files
import kotlin.io.path.readText

fun main() {
    val root = ProjectUtils.findProjectRootDir()!!
    val importDirective = "import ai.platon.pulsar.common.logPrintln()"

    Files.walk(root).filter { it.toString().contains("src/test/kotlin") }
        .filter { it.fileName.toString().endsWith(".kt") }.forEach { path ->
            val content = path.readText()

            if (content.contains("logPrintln") && !content.contains(importDirective)) {
                val lines = content.split("\n").toMutableList()

                var pos = -1
                lines.forEachIndexed { index, line ->
                    if (line.startsWith("import ai.")) {
                        pos = index
                        return@forEachIndexed
                    }
                }

                if (pos > 0) {
                    lines.add(importDirective)
                    Files.write(path, lines)
                }
            }
        }
}
