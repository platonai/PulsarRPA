package ai.platon.pulsar.common

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

object LinkExtractors {

    private val urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                    + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                    + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)

    fun extractFromResource(resource: String): Set<String> {
        val urls = mutableSetOf<String>()
        ResourceLoader.readAllLines(resource).forEach {
            extractUrlsFromLineTo(it, urls)
        }
        return urls
    }

    fun extractFromFiles(path: Path): Set<String> {
        val urls = mutableSetOf<String>()
        Files.list(path).filter { Files.isRegularFile(it) }.forEach { resourcePath ->
            Files.newBufferedReader(resourcePath).forEachLine {
                extractUrlsFromLineTo(it, urls)
            }
        }
        return urls
    }

    private fun extractUrlsFromLineTo(line: String, urls: MutableSet<String>) {
        val matcher = urlPattern.matcher(line)
        while (matcher.find()) {
            val start = matcher.start(1)
            val end = matcher.end()
            urls.add((line.substring(start, end)))
        }
    }
}
