package ai.platon.scent.common

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.url.Hyperlink
import org.slf4j.LoggerFactory
import java.nio.file.Path

interface ExternalHyperlinkLoader {
    fun load()
    fun asIterable(): Iterable<Hyperlink>
}

open class LocalFileHyperlinkLoader(val path: Path): ExternalHyperlinkLoader {
    private val log = LoggerFactory.getLogger(LocalFileHyperlinkLoader::class.java)

    val fetchUrls = mutableListOf<Hyperlink>()

    override fun load() {
        kotlin.runCatching {
            LinkExtractors.fromFile(path).mapTo(fetchUrls) { Hyperlink(it) }
        }.onFailure { log.warn("Failed to load urls from $path") }
    }

    override fun asIterable(): Iterable<Hyperlink> {
        return fetchUrls.asIterable()
    }
}
