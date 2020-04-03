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

    fun extractFromAllFiles(path: Path): Set<String> {
        val links = mutableSetOf<String>()
        Files.list(path).filter { Files.isRegularFile(it) }.forEach {
            Files.newBufferedReader(it).forEachLine {
                val matcher = urlPattern.matcher(it)
                while (matcher.find()) {
                    val start = matcher.start(1)
                    val end = matcher.end()
                    links.add((it.substring(start, end)))
                }
            }
        }

        return links
    }
}
