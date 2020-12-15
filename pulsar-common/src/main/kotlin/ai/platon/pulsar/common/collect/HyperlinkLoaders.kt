package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.common.url.UrlAware
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

interface ExternalUrlLoader {
    fun save(url: UrlAware)
    fun saveAll(urls: Iterable<UrlAware>)
    fun loadTo(sink: MutableCollection<UrlAware>)
}

open class LocalFileUrlLoader(val path: Path): ExternalUrlLoader {
    private val log = LoggerFactory.getLogger(LocalFileUrlLoader::class.java)

    val fetchUrls = mutableListOf<UrlAware>()

    override fun save(url: UrlAware) {
        Files.writeString(path, "$url\n", StandardOpenOption.APPEND)
    }

    override fun saveAll(urls: Iterable<UrlAware>) {
        Files.writeString(path, urls.joinToString("\n"), StandardOpenOption.APPEND)
    }

    override fun loadTo(sink: MutableCollection<UrlAware>) {
        kotlin.runCatching {
            LinkExtractors.fromFile(path).mapTo(sink) { Hyperlink(it) }
        }.onFailure { log.warn("Failed to load urls from $path") }
    }
}
