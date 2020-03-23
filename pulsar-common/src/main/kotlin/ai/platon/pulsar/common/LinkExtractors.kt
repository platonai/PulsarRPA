package ai.platon.pulsar.common

import java.nio.file.Files
import java.nio.file.Path

object LinkExtractors {
    fun extractFromAllFiles(path: Path): Set<String> {
        val links = mutableSetOf<String>()
        Files.list(path).filter { Files.isRegularFile(it) }.forEach {
            Files.newBufferedReader(it).forEachLine {
                val start = it.indexOf("http")
                if (start > 0) {
                    val end = it.indexOfAny(charArrayOf(' '), start, true)
                    if (end > 0) {
                        links.add(it.substring(start, end))
                    } else {
                        links.add(it.substring(start))
                    }
                }
            }
        }

        return links
    }
}
