package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.UrlExtractor
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.HyperlinkDatum
import ai.platon.pulsar.common.urls.UrlAware
import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

open class LocalFileUrlLoader(val path: Path): AbstractExternalUrlLoader() {
    private val log = LoggerFactory.getLogger(LocalFileUrlLoader::class.java)
    private val delimiter = "\t"
    private val gson = GsonBuilder().create()
    private val urlExtractor = UrlExtractor()

    val fetchUrls = mutableListOf<Hyperlink>()

    override fun save(url: UrlAware, group: Int) {
        val hyperlink = if (url is Hyperlink) url else Hyperlink(url)
        val json = gson.toJson(hyperlink.data())
        Files.writeString(path, "$group$delimiter$json\n", StandardOpenOption.APPEND)
    }

    override fun loadToNow(sink: MutableCollection<UrlAware>, maxSize: Int, group: Int, priority: Int): Collection<UrlAware> {
        when {
            maxSize < 0 -> throw IllegalArgumentException("maxSize should be >= 0")
            maxSize == 0 -> return listOf()
        }

        val g = "$group"
        runCatching {
            Files.readAllLines(path).asSequence().take(maxSize).mapNotNullTo(sink) { parse(it, g) }
        }.onFailure { log.warn("Failed to load urls from $path", it) }

        return sink
    }

    override fun <T> loadToNow(sink: MutableCollection<T>, maxSize: Int, group: Int, priority: Int, transformer: (UrlAware) -> T): Collection<T> {
        when {
            maxSize < 0 -> throw IllegalArgumentException("maxSize should be >= 0")
            maxSize == 0 -> return listOf()
        }

        val g = "$group"
        runCatching {
            Files.readAllLines(path).asSequence().take(maxSize).mapNotNull { parse(it, g) }.mapTo(sink) { transformer(it) }
        }.onFailure { log.warn("Failed to load urls from $path", it) }

        return sink
    }

    private fun parse(line: String, group: String): Hyperlink? {
        val parts = line.split(delimiter)
        return if (parts.size == 2 && parts[0] == group) {
            val data = gson.fromJson(parts[1], HyperlinkDatum::class.java)
            Hyperlink(data)
        } else {
            urlExtractor.extract(line)?.let { Hyperlink(it) }
        }
    }
}

open class TemporaryLocalFileUrlLoader: LocalFileUrlLoader(
        Files.createTempFile("hyperlink", ".txt")
)
