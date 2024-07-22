package ai.platon.pulsar.common

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * TODO: org.nibor.autolink.LinkExtractor might be faster
 * */
open class UrlExtractor {
    companion object {
        
        /**
         * TODO: see https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
         * */
        val URL_PATTERN: Pattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                    + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                    + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)
    }

    fun extract(line: String): String? {
        val matcher = URL_PATTERN.matcher(line)
        while (matcher.find()) {
            val start = matcher.start(1)
            val end = matcher.end()
            return line.substring(start, end)
        }
        return null
    }

    fun extractTo(line: String, urls: MutableSet<String>) {
        val matcher = URL_PATTERN.matcher(line)
        while (matcher.find()) {
            val start = matcher.start(1)
            val end = matcher.end()
            urls.add(line.substring(start, end))
        }
    }
}

internal class ResourceExtractor(
    val resource: String,
    val filter: (String) -> Boolean = { true }
): UrlExtractor() {
    fun extract(): Set<String> {
        val urls = mutableSetOf<String>()
        ResourceLoader.readAllLines(resource, filter).forEach { extractTo(it, urls) }
        return urls
    }
}

internal class FileExtractor(
    val path: Path,
    val filter: (String) -> Boolean = { true }
): UrlExtractor() {
    fun extract(): Set<String> {
        if (!Files.exists(path)) {
            return setOf()
        }

        val urls = mutableSetOf<String>()
        Files.readAllLines(path).filter(filter).forEach { extractTo(it, urls) }
        return urls
    }
}

internal class DirectoryExtractor(
    val baseDir: Path,
    val filter: (String) -> Boolean = { true }
): UrlExtractor() {
    fun extract(): Set<String> {
        if (!Files.exists(baseDir)) {
            return setOf()
        }

        val urls = mutableSetOf<String>()
        Files.list(baseDir).filter { Files.isRegularFile(it) }.forEach { path ->
            Files.newBufferedReader(path).forEachLine {
                extractTo(it, urls)
            }
        }
        return urls
    }
}

object LinkExtractors {
    @JvmStatic
    fun fromResource(resource: String) = ResourceExtractor(resource).extract()
    @JvmStatic
    fun fromFile(path: Path) = FileExtractor(path).extract()
    @JvmStatic
    fun fromFile(path: String) = FileExtractor(Paths.get(path)).extract()
    @JvmStatic
    fun fromDirectory(baseDir: Path) = DirectoryExtractor(baseDir).extract()
    @JvmStatic
    fun fromDirectory(baseDir: String) = DirectoryExtractor(Paths.get(baseDir)).extract()
}
