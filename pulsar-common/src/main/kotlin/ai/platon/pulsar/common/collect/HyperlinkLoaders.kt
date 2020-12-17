package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.common.url.UrlAware
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

interface ExternalUrlLoader {
    /**
     * The cache size. No more items should be loaded into the memory if the cache is full.
     * */
    var cacheSize: Int
    /**
     * Save the url to the external repository
     * */
    fun save(url: UrlAware)
    /**
     * Save all the url to the external repository
     * */
    fun saveAll(urls: Iterable<UrlAware>)
    /**
     * If there are more items in the source
     * */
    fun hasMore(): Boolean
    /**
     * Load items from the source to the sink
     * */
    fun loadTo(sink: MutableCollection<UrlAware>)
}

abstract class AbstractExternalUrlLoader: ExternalUrlLoader {
    override var cacheSize: Int = Int.MAX_VALUE
    override fun hasMore(): Boolean = true
    abstract override fun save(url: UrlAware)
    abstract override fun saveAll(urls: Iterable<UrlAware>)
    abstract override fun loadTo(sink: MutableCollection<UrlAware>)
}

open class LocalFileUrlLoader(val path: Path): AbstractExternalUrlLoader() {
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
