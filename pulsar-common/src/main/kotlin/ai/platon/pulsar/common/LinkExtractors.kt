package ai.platon.pulsar.common

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class ResourceExtractor(
    val resource: String,
    val filter: (String) -> Boolean = { true }
) : UrlExtractor() {
    fun extract(): Set<String> {
        val urls = mutableSetOf<String>()
        ResourceLoader.readAllLines(resource, filter).forEach { extractTo(it, urls, filter) }
        return urls
    }
}

internal class FileExtractor(
    val path: Path,
    val filter: (String) -> Boolean = { true }
) : UrlExtractor() {
    fun extract(): Set<String> {
        if (!Files.exists(path)) {
            return setOf()
        }

        val urls = mutableSetOf<String>()
        Files.readAllLines(path).filter(filter).forEach { extractTo(it, urls, filter) }
        return urls
    }
}

internal class DirectoryExtractor(
    val baseDir: Path,
    val filter: (String) -> Boolean = { true }
) : UrlExtractor() {
    fun extract(): Set<String> {
        if (!Files.exists(baseDir)) {
            return setOf()
        }

        val urls = mutableSetOf<String>()
        Files.list(baseDir).filter { Files.isRegularFile(it) }.forEach { path ->
            Files.newBufferedReader(path).forEachLine {
                extractTo(it, urls, filter)
            }
        }
        return urls
    }
}

object LinkExtractors {
    @JvmStatic
    fun fromResource(resource: String) = ResourceExtractor(resource).extract()

    @JvmStatic
    fun fromResource(resource: String, filter: (String) -> Boolean) = ResourceExtractor(resource, filter).extract()

    @JvmStatic
    fun fromFile(path: Path) = FileExtractor(path).extract()

    @JvmStatic
    fun fromFile(path: Path, filter: (String) -> Boolean) = FileExtractor(path, filter).extract()

    @JvmStatic
    fun fromFile(path: String) = FileExtractor(Paths.get(path)).extract()

    @JvmStatic
    fun fromFile(path: String, filter: (String) -> Boolean) = FileExtractor(Paths.get(path), filter).extract()

    @JvmStatic
    fun fromDirectory(baseDir: Path) = DirectoryExtractor(baseDir).extract()

    @JvmStatic
    fun fromDirectory(baseDir: Path, filter: (String) -> Boolean) = DirectoryExtractor(baseDir, filter).extract()

    @JvmStatic
    fun fromDirectory(baseDir: String) = DirectoryExtractor(Paths.get(baseDir)).extract()

    @JvmStatic
    fun fromDirectory(baseDir: String, filter: (String) -> Boolean) = DirectoryExtractor(Paths.get(baseDir), filter).extract()
}
